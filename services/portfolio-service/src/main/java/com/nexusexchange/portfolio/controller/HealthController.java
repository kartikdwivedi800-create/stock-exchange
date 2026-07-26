package com.nexusexchange.portfolio.controller;

import com.nexusexchange.portfolio.dto.response.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        return ResponseEntity.ok(HealthResponse.builder()
                .service("portfolio-service")
                .status("UP")
                .build());
    }
}
