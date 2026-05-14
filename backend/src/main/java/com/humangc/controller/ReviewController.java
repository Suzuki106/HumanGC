package com.humangc.controller;

import com.humangc.dto.ReviewResponse;
import com.humangc.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/review/{paperId}")
    public ReviewResponse review(@PathVariable Long paperId) {
        log.info("Review request for paperId={}", paperId);
        String reviewText = reviewService.review(paperId);
        return new ReviewResponse(paperId, reviewText);
    }
}
