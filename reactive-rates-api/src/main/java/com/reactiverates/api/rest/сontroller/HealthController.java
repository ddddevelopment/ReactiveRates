package com.reactiverates.api.rest.сontroller;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    @GetMapping
    public Mono<Map<String, Object>> health() {
        return Mono.just(Map.of(
            "status", "UP", 
            "service", "ReactiveRates API",
            "timestamp", LocalDateTime.now(),
            "version", "1.0.0"
            ));
    }
}
