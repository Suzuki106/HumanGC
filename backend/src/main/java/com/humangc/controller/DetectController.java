package com.humangc.controller;

import com.humangc.dto.DetectResponse;
import com.humangc.service.HumanRateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin
public class DetectController {

    @Autowired
    private HumanRateService humanRateService;

    @PostMapping("/detect/{paperId}")
    public DetectResponse detect(@PathVariable Long paperId) {
        log.info("Detect request for paperId={}", paperId);
        return humanRateService.calculateHumanRate(paperId);
    }
}
