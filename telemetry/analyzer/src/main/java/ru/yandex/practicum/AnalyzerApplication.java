package ru.yandex.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import ru.yandex.practicum.processor.HubEventProcessor;
import ru.yandex.practicum.processor.SnapshotProcessor;

@SpringBootApplication
@EnableDiscoveryClient
public class AnalyzerApplication {
    public static void main(String[] args) {
        System.out.println("=== GITHUB_DEBUG_MAIN_START ===");
        System.out.println("🏁 Запуск AnalyzerApplication...");
        SpringApplication.run(AnalyzerApplication.class, args);
        System.out.println("✅ SpringApplication запущен");
    }
}