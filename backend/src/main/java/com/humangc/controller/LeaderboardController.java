package com.humangc.controller;

import com.humangc.dto.LeaderboardResponse;
import com.humangc.service.LeaderboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin
public class LeaderboardController {

    @Autowired
    private LeaderboardService leaderboardService;

    @GetMapping("/leaderboard")
    public LeaderboardResponse leaderboard(
            @RequestParam(defaultValue = "person") String type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        log.info("Leaderboard request: type={}, page={}, size={}", type, page, size);
        return leaderboardService.getLeaderboard(type, page, size);
    }
}
