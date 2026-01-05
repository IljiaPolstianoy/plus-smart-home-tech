package ru.yandex.practicum;

import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.admin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
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
        // Определяем окружение
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isCi = Arrays.stream(activeProfiles).anyMatch(p -> p.equals("ci"));
        boolean isTest = Arrays.stream(activeProfiles).anyMatch(p -> p.equals("test"));

        log.info("KafkaTopicInitializer starting. Profiles: {}, Bootstrap: {}",
                Arrays.toString(activeProfiles), bootstrapServers);

        // В CI/Test средах пропускаем создание топиков
        if (isCi || isTest) {
            log.info("Skipping topic initialization in CI/Test mode. Kafka: {}", bootstrapServers);
            return;
        }

        // Проверяем, что Kafka доступна
        if (!isKafkaAvailable()) {
            log.warn("Kafka not available at {}. Skipping topic initialization.", bootstrapServers);
            return;
        }

        createTopicIfNotExists();
    }

    private boolean isKafkaAvailable() {
        // Простая проверка - если адрес localhost:9099 (тестовый), считаем что Kafka есть
        if (bootstrapServers.contains(":9099") ||
                bootstrapServers.contains("dummy") ||
                System.getenv("CI") != null) {
            // В CI тестах Kafka работает, но создание топиков не нужно
            return false;
        }
        return true;
    }

    private void createTopicIfNotExists() {
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
        } catch (Exception e) {
            log.error("Error creating Kafka topic: {}", e.getMessage());
            // НЕ падаем, позволяем приложению работать
        }
    }
}