package com.humangc.controller;

import com.humangc.dto.ServerStatusResponse;
import com.humangc.service.ServerStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin
public class ServerStatusController {

    @Autowired
    private ServerStatusService serverStatusService;

    @GetMapping("/server-status")
    public ServerStatusResponse serverStatus() {
        log.info("Server status request");
        return serverStatusService.getStatus();
    }
}
