package ru.yandex.practicum;

import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class KafkaTopicInitializer implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    private boolean isTestProfile = false;

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        // Проверяем, запущен ли тестовый профиль
        String[] activeProfiles = event.getEnvironment().getActiveProfiles();
        isTestProfile = Arrays.stream(activeProfiles)
                .anyMatch(profile -> profile.equals("test") || profile.equals("ci"));
    }

    @PostConstruct
    public void init() {
        if (isTestProfile) {
            System.out.println("Test profile detected. Skipping Kafka topic initialization.");
            return;
        }

        System.out.println("Initializing Kafka topics with bootstrap servers: " + bootstrapServers);

        try (AdminClient admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000
        ))) {
            ListTopicsResult topics = admin.listTopics();
            Set<String> topicNames = topics.names().get(5, TimeUnit.SECONDS);

            if (!topicNames.contains("telemetry.snapshots.v1")) {
                NewTopic newTopic = new NewTopic("telemetry.snapshots.v1", 1, (short) 1);
                admin.createTopics(List.of(newTopic)).all().get(10, TimeUnit.SECONDS);
                System.out.println("Topic telemetry.snapshots.v1 created");
            }
        } catch (Exception e) {
            // В тестах только логируем, в продакшене - выбрасываем исключение
            if (isTestProfile) {
                System.err.println("Kafka not available in tests: " + e.getMessage());
            } else {
                throw new RuntimeException("Failed to initialize Kafka topics", e);
            }
        }
    }
}