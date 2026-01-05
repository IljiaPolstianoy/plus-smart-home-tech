package ru.yandex.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.processor.HubRouterClientService;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@Slf4j
@RequiredArgsConstructor
public class HealthController {

    private final HubRouterClientService hubRouterClient;

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        log.debug("Health check requested");

        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "analyzer");
        health.put("timestamp", Instant.now().toString());
        health.put("grpc", hubRouterClient.getGrpcStatus());
        health.put("grpcAvailable", hubRouterClient.isGrpcAvailable());

        return ResponseEntity.ok(health);
    }

    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debug() {
        log.info("Debug health check");

        Map<String, Object> debugInfo = new HashMap<>();
        debugInfo.put("timestamp", Instant.now());
        debugInfo.put("grpc", hubRouterClient.getGrpcStatus());

        // Тестовый gRPC вызов
        try {
            debugInfo.put("grpcTest", "configured");
        } catch (Exception e) {
            debugInfo.put("grpcTest", "error: " + e.getMessage());
        }

        return ResponseEntity.ok(debugInfo);
    }
}