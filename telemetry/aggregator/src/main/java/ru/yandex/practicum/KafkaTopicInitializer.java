package ru.yandex.practicum;

import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.admin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class KafkaTopicInitializer {

    private static final Logger log = LoggerFactory.getLogger(KafkaTopicInitializer.class);

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    private final Environment environment;

    public KafkaTopicInitializer(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        // Проверяем, запущен ли CI или тестовый режим
        boolean isCi = "true".equals(System.getenv("CI")) ||
                "true".equals(System.getenv("GITHUB_ACTIONS"));

        // Проверяем активные профили
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isTestProfile = false;
        for (String profile : activeProfiles) {
            if (profile.contains("test") || profile.contains("ci")) {
                isTestProfile = true;
                break;
            }
        }

        // В CI/тестах пропускаем создание топиков
        if (isCi || isTestProfile) {
            log.info("Skipping Kafka topic initialization in CI/test mode");
            return;
        }

        log.info("Initializing Kafka topics with bootstrap servers: {}", bootstrapServers);

        try {
            // Быстрая проверка доступности Kafka
            if (!isKafkaAvailable()) {
                log.warn("Kafka is not available at {}. Skipping topic initialization.", bootstrapServers);
                return;
            }

            try (AdminClient admin = AdminClient.create(Map.of(
                    AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                    AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000
            ))) {
                ListTopicsResult topics = admin.listTopics();
                Set<String> topicNames = topics.names().get(5, TimeUnit.SECONDS);

                if (!topicNames.contains("telemetry.snapshots.v1")) {
                    NewTopic newTopic = new NewTopic("telemetry.snapshots.v1", 1, (short) 1);
                    admin.createTopics(List.of(newTopic)).all().get(10, TimeUnit.SECONDS);
                    log.info("Topic telemetry.snapshots.v1 created");
                } else {
                    log.info("Topic telemetry.snapshots.v1 already exists");
                }
            }
        } catch (Exception e) {
            log.error("Error during Kafka topic initialization: {}", e.getMessage());
            // Не падаем, позволяем приложению запуститься
            // В CI это нормально, так как Kafka может быть недоступна
        }
    }

    private boolean isKafkaAvailable() {
        // Простая проверка формата адреса
        if (bootstrapServers == null || bootstrapServers.trim().isEmpty()) {
            log.warn("Kafka bootstrap servers not configured");
            return false;
        }

        // В CI всегда возвращаем false, чтобы не пытаться подключаться
        if ("true".equals(System.getenv("CI")) ||
                "true".equals(System.getenv("GITHUB_ACTIONS"))) {
            return false;
        }

        return true;
    }
}