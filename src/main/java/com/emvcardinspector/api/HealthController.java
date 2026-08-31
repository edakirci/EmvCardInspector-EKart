package com.emvcardinspector.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Minimal endpoint used by the desktop UI to verify the local backend connection. */
@RestController
@RequestMapping("/api")
public final class HealthController {
    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok", "EMV Card Inspector backend is ready");
    }

    public record HealthResponse(String status, String message) {
    }
}
