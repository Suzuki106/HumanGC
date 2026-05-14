package com.humangc.controller;

import com.humangc.dto.DonateRequest;
import com.humangc.dto.ServerStatusResponse;
import com.humangc.service.ServerStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin
public class DonateController {

    @Autowired
    private ServerStatusService serverStatusService;

    @PostMapping("/donate")
    public ServerStatusResponse donate(@RequestBody DonateRequest request) {
        log.info("Donate request: amount={}, anonymousId={}", request.getAmount(), request.getAnonymousId());
        return serverStatusService.donate(request.getAmount(), request.getAnonymousId());
    }
}
