package com.humangc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.humangc.dto.ServerStatusResponse;
import com.humangc.entity.Donation;
import com.humangc.entity.ServerStatus;
import com.humangc.entity.User;
import com.humangc.mapper.DonationMapper;
import com.humangc.mapper.ServerStatusMapper;
import com.humangc.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ServerStatusService {

    @Autowired
    private ServerStatusMapper serverStatusMapper;

    @Autowired
    private DonationMapper donationMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * Get current server status with remaining time and progress percent.
     * The server starts at startTime and ends at endTime.
     * If no status record exists, create a default one.
     */
    public ServerStatusResponse getStatus() {
        List<ServerStatus> statuses = serverStatusMapper.selectList(null);
        ServerStatus status;

        if (statuses.isEmpty()) {
            // Initialize default status: 30 days from now
            status = new ServerStatus();
            status.setStartTime(LocalDateTime.now());
            status.setEndTime(LocalDateTime.now().plusDays(30));
            status.setUpdatedAt(LocalDateTime.now());
            serverStatusMapper.insert(status);
        } else {
            status = statuses.get(0);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = status.getStartTime();
        LocalDateTime end = status.getEndTime();

        // If end time has passed, server is done
        if (now.isAfter(end)) {
            return new ServerStatusResponse(0L, BigDecimal.valueOf(100.0));
        }

        // Calculate remaining days
        long daysRemaining = Duration.between(now, end).toDays();

        // Calculate progress percent (elapsed / total duration)
        long totalSeconds = Duration.between(start, end).getSeconds();
        long elapsedSeconds = Duration.between(start, now).getSeconds();

        double progressPercent;
        if (totalSeconds > 0) {
            progressPercent = Math.min((double) elapsedSeconds / totalSeconds * 100.0, 99.9);
        } else {
            progressPercent = 0;
        }

        return new ServerStatusResponse(
                Math.max(daysRemaining, 0),
                BigDecimal.valueOf(progressPercent).setScale(2, RoundingMode.HALF_UP)
        );
    }

    /**
     * Process a donation: extend server end time by some hours.
     * Each unit of donation extends by a calculated amount of hours.
     */
    @Transactional
    public ServerStatusResponse donate(BigDecimal amount, String anonymousId) {
        log.info("Processing donation: amount={}, anonymousId={}", amount, anonymousId);

        // Find or create user
        User user = findOrCreateUser(anonymousId);

        // Calculate extended hours: 1 unit = 1 hour (simple formula)
        // In real scenario, amount is currency; here we map amount directly to hours
        int extendedHours = amount.intValue();

        // Get current server status
        List<ServerStatus> statuses = serverStatusMapper.selectList(null);
        ServerStatus status;

        if (statuses.isEmpty()) {
            status = new ServerStatus();
            status.setStartTime(LocalDateTime.now());
            status.setEndTime(LocalDateTime.now().plusDays(30));
        } else {
            status = statuses.get(0);
        }

        // Extend end time
        LocalDateTime newEndTime = status.getEndTime();
        if (newEndTime.isBefore(LocalDateTime.now())) {
            // If already expired, restart from now
            newEndTime = LocalDateTime.now().plusHours(extendedHours);
        } else {
            newEndTime = newEndTime.plusHours(extendedHours);
        }
        status.setEndTime(newEndTime);
        status.setUpdatedAt(LocalDateTime.now());
        serverStatusMapper.updateById(status);

        // Save donation record
        Donation donation = new Donation();
        donation.setUserId(user.getId());
        donation.setAmount(amount);
        donation.setExtendedHours(extendedHours);
        donation.setCreatedAt(LocalDateTime.now());
        donationMapper.insert(donation);

        log.info("Donation processed: extended by {} hours, new end time: {}", extendedHours, newEndTime);

        return getStatus();
    }

    private User findOrCreateUser(String anonymousId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getAnonymousId, anonymousId);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            user = new User();
            user.setAnonymousId(anonymousId);
            user.setCreatedAt(LocalDateTime.now());
            userMapper.insert(user);
        }

        return user;
    }
}
