package ru.yandex.practicum.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessorInitializer implements ApplicationRunner {

    private final SnapshotProcessor snapshotProcessor;
    private final HubEventProcessor hubEventProcessor;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("=== GITHUB_DEBUG_PROCESSOR_INITIALIZER ===");
        log.info("=== НАЧАЛО ИНИЦИАЛИЗАЦИИ PROCESSORS ===");

        // Проверяем зависимости
        System.out.println("Проверка зависимостей:");
        System.out.println("  hubEventProcessor: " + (hubEventProcessor != null ? "OK" : "NULL"));
        System.out.println("  snapshotProcessor: " + (snapshotProcessor != null ? "OK" : "NULL"));

        // Ждем немного
        Thread.sleep(2000);

        // Запускаем процессоры
        System.out.println("Запускаем процессоры...");
        hubEventProcessor.start();
        snapshotProcessor.start();

        // Активируем
        hubEventProcessor.setInitialized(true);
        snapshotProcessor.setInitialized(true);

        System.out.println("✅ Процессоры запущены и инициализированы");
        log.info("=== ИНИЦИАЛИЗАЦИЯ PROCESSORS ЗАВЕРШЕНА ===");
    }
}