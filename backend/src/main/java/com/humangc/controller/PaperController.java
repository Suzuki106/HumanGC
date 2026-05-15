package com.humangc.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.humangc.entity.Paper;
import com.humangc.entity.User;
import com.humangc.mapper.PaperMapper;
import com.humangc.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin
public class PaperController {

    @Autowired
    private PaperMapper paperMapper;

    @Autowired
    private UserMapper userMapper;

    @GetMapping("/paper/{id}")
    public Map<String, Object> getPaper(@PathVariable Long id) {
        log.info("Get paper detail for id={}", id);

        Paper paper = paperMapper.selectById(id);
        if (paper == null) {
            throw new RuntimeException("Paper not found: " + id);
        }

        User user = userMapper.selectById(paper.getUserId());

        Map<String, Object> result = new HashMap<>();
        result.put("paper", paper);
        result.put("user", user);

        return result;
    }

    @GetMapping("/paper/user/{anonymousId}")
    public List<Paper> getUserPapers(@PathVariable String anonymousId) {
        log.info("Get papers for user anonymousId={}", anonymousId);

        // Find user
        LambdaQueryWrapper<User> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(User::getAnonymousId, anonymousId);
        User user = userMapper.selectOne(userWrapper);

        if (user == null) {
            return List.of();
        }

        // Get papers
        LambdaQueryWrapper<Paper> paperWrapper = new LambdaQueryWrapper<>();
        paperWrapper.eq(Paper::getUserId, user.getId())
                    .orderByDesc(Paper::getCreatedAt);
        return paperMapper.selectList(paperWrapper);
    }
}
