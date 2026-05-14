package com.humangc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.humangc.dto.LeaderboardEntry;
import com.humangc.dto.LeaderboardResponse;
import com.humangc.entity.Leaderboard;
import com.humangc.entity.Paper;
import com.humangc.entity.User;
import com.humangc.mapper.LeaderboardMapper;
import com.humangc.mapper.PaperMapper;
import com.humangc.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LeaderboardService {

    @Autowired
    private PaperMapper paperMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LeaderboardMapper leaderboardMapper;

    /**
     * Get leaderboard by type with pagination.
     * Types: person, region, school
     */
    public LeaderboardResponse getLeaderboard(String type, Integer page, Integer size) {
        log.info("Getting leaderboard for type={}, page={}, size={}", type, page, size);

        // Recalculate first
        recalculate(type);

        // Query from leaderboard table
        LambdaQueryWrapper<Leaderboard> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Leaderboard::getType, type)
               .orderByAsc(Leaderboard::getRankNum);

        Page<Leaderboard> leaderboardPage = new Page<>(page, size);
        leaderboardPage = leaderboardMapper.selectPage(leaderboardPage, wrapper);

        List<LeaderboardEntry> entries = leaderboardPage.getRecords().stream()
                .map(lb -> new LeaderboardEntry(
                        lb.getRankNum(),
                        lb.getEntityName(),
                        lb.getAvgHumanRate(),
                        lb.getPaperCount()))
                .collect(Collectors.toList());

        return new LeaderboardResponse(type, entries, (int) leaderboardPage.getTotal(), page, size);
    }

    /**
     * Recalculate leaderboard rankings for a given type.
     */
    @Transactional
    public void recalculate(String type) {
        log.info("Recalculating leaderboard for type={}", type);

        // Clear old entries for this type
        LambdaQueryWrapper<Leaderboard> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(Leaderboard::getType, type);
        leaderboardMapper.delete(deleteWrapper);

        // Aggregate data
        List<LeaderboardEntry> aggregated = new ArrayList<>();

        switch (type) {
            case "person":
                aggregated = aggregateByPerson();
                break;
            case "region":
                aggregated = aggregateByRegion();
                break;
            case "school":
                aggregated = aggregateBySchool();
                break;
            default:
                throw new RuntimeException("Unknown leaderboard type: " + type);
        }

        // Sort by avg human rate descending (higher human rate = higher rank)
        aggregated.sort((a, b) -> b.getAvgHumanRate().compareTo(a.getAvgHumanRate()));

        // Save to leaderboard table
        int rank = 1;
        for (LeaderboardEntry entry : aggregated) {
            Leaderboard lb = new Leaderboard();
            lb.setType(type);
            lb.setRankNum(rank++);
            lb.setEntityName(entry.getName());
            lb.setAvgHumanRate(entry.getAvgHumanRate());
            lb.setPaperCount(entry.getPaperCount());
            lb.setUpdatedAt(LocalDateTime.now());
            leaderboardMapper.insert(lb);
        }

        log.info("Leaderboard recalculated for type={}, {} entries", type, aggregated.size());
    }

    /**
     * Aggregate human rate by person (anonymous user).
     */
    private List<LeaderboardEntry> aggregateByPerson() {
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Paper::getHumanRate);
        List<Paper> papers = paperMapper.selectList(wrapper);

        // Group by userId
        Map<Long, List<Paper>> grouped = papers.stream()
                .collect(Collectors.groupingBy(Paper::getUserId));

        List<LeaderboardEntry> entries = new ArrayList<>();
        for (Map.Entry<Long, List<Paper>> entry : grouped.entrySet()) {
            Long userId = entry.getKey();
            List<Paper> userPapers = entry.getValue();

            double avgRate = userPapers.stream()
                    .map(p -> p.getHumanRate().doubleValue())
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0);

            User user = userMapper.selectById(userId);
            String name = user != null && user.getAnonymousId() != null
                    ? user.getAnonymousId()
                    : "匿名用户" + userId;

            entries.add(new LeaderboardEntry(0, name,
                    BigDecimal.valueOf(avgRate).setScale(1, RoundingMode.HALF_UP),
                    userPapers.size()));
        }

        return entries;
    }

    /**
     * Aggregate human rate by region.
     */
    private List<LeaderboardEntry> aggregateByRegion() {
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Paper::getHumanRate);
        List<Paper> papers = paperMapper.selectList(wrapper);

        // Map userId -> region
        Map<Long, String> userRegionMap = new HashMap<>();
        for (Paper paper : papers) {
            if (!userRegionMap.containsKey(paper.getUserId())) {
                User user = userMapper.selectById(paper.getUserId());
                if (user != null && user.getRegion() != null && !user.getRegion().isBlank()) {
                    userRegionMap.put(paper.getUserId(), user.getRegion());
                } else {
                    userRegionMap.put(paper.getUserId(), "未知地区");
                }
            }
        }

        // Group by region
        Map<String, List<Paper>> grouped = new HashMap<>();
        for (Paper paper : papers) {
            String region = userRegionMap.getOrDefault(paper.getUserId(), "未知地区");
            grouped.computeIfAbsent(region, k -> new ArrayList<>()).add(paper);
        }

        List<LeaderboardEntry> entries = new ArrayList<>();
        for (Map.Entry<String, List<Paper>> entry : grouped.entrySet()) {
            List<Paper> regionPapers = entry.getValue();
            double avgRate = regionPapers.stream()
                    .map(p -> p.getHumanRate().doubleValue())
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0);

            entries.add(new LeaderboardEntry(0, entry.getKey(),
                    BigDecimal.valueOf(avgRate).setScale(1, RoundingMode.HALF_UP),
                    regionPapers.size()));
        }

        return entries;
    }

    /**
     * Aggregate human rate by school.
     */
    private List<LeaderboardEntry> aggregateBySchool() {
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Paper::getHumanRate);
        List<Paper> papers = paperMapper.selectList(wrapper);

        // Map userId -> school
        Map<Long, String> userSchoolMap = new HashMap<>();
        for (Paper paper : papers) {
            if (!userSchoolMap.containsKey(paper.getUserId())) {
                User user = userMapper.selectById(paper.getUserId());
                if (user != null && user.getSchool() != null && !user.getSchool().isBlank()) {
                    userSchoolMap.put(paper.getUserId(), user.getSchool());
                } else {
                    userSchoolMap.put(paper.getUserId(), "未知学校");
                }
            }
        }

        // Group by school
        Map<String, List<Paper>> grouped = new HashMap<>();
        for (Paper paper : papers) {
            String school = userSchoolMap.getOrDefault(paper.getUserId(), "未知学校");
            grouped.computeIfAbsent(school, k -> new ArrayList<>()).add(paper);
        }

        List<LeaderboardEntry> entries = new ArrayList<>();
        for (Map.Entry<String, List<Paper>> entry : grouped.entrySet()) {
            List<Paper> schoolPapers = entry.getValue();
            double avgRate = schoolPapers.stream()
                    .map(p -> p.getHumanRate().doubleValue())
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0);

            entries.add(new LeaderboardEntry(0, entry.getKey(),
                    BigDecimal.valueOf(avgRate).setScale(1, RoundingMode.HALF_UP),
                    schoolPapers.size()));
        }

        return entries;
    }
}
