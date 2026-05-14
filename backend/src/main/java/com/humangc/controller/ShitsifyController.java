package com.humangc.controller;

import com.humangc.dto.ShitsifyRequest;
import com.humangc.dto.ShitsifyResponse;
import com.humangc.service.ShitsifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin
public class ShitsifyController {

    @Autowired
    private ShitsifyService shitsifyService;

    @PostMapping("/shitsify/{paperId}")
    public ShitsifyResponse shitsify(@PathVariable Long paperId, @RequestBody ShitsifyRequest request) {
        log.info("Shitsify request for paperId={}, style={}", paperId, request.getStyle());
        return shitsifyService.generateShitPaper(paperId, request.getStyle());
    }
}
