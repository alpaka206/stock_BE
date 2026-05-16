package com.alpaka.stock.api;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/readyz")
    public Map<String, Object> readiness() {
        return Map.of(
            "status",
            "ready",
            "asOf",
            OffsetDateTime.now(ZoneOffset.UTC).toString()
        );
    }
}
