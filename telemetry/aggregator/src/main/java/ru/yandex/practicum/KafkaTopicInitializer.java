package ru.yandex.practicum;

import jakarta.annotation.PostConstruct;
import org.apache.kafka.clients.admin.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;


@Component
public class KafkaTopicInitializer {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @PostConstruct
    public void init() {
        try (AdminClient admin = AdminClient.create(Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers
        ))) {
            // Проверяем существование топика snapshots
            ListTopicsResult topics = admin.listTopics();
            Set<String> topicNames = topics.names().get();

            if (!topicNames.contains("telemetry.snapshots.v1")) {
                NewTopic newTopic = new NewTopic("telemetry.snapshots.v1", 1, (short) 1);
                admin.createTopics(List.of(newTopic)).all().get();
                System.out.println("Топик telemetry.snapshots.v1 создан");
            }
        } catch (Exception e) {
            System.err.println("Ошибка при создании топика: " + e.getMessage());
        }
    }
}