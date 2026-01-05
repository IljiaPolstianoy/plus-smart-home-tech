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
        System.out.println("\n\n=== GITHUB_DEBUG_ANALYZER_START ===");
        System.out.println("🚀 Анализатор запускается...");
        System.out.println("HubEventProcessor: " + (hubEventProcessor != null ? "OK" : "NULL"));
        System.out.println("SnapshotProcessor: " + (snapshotProcessor != null ? "OK" : "NULL"));

        // Минимальная задержка
        Thread.sleep(2000);

        System.out.println("🔄 Запускаем процессоры...");
        hubEventProcessor.start();
        snapshotProcessor.start();

        System.out.println("✅ Процессоры запущены");

        // Даем время на запуск потоков
        Thread.sleep(3000);

        System.out.println("⚡ Активируем процессоры...");
        hubEventProcessor.setInitialized(true);
        snapshotProcessor.setInitialized(true);

        System.out.println("✅ Анализатор готов к работе!");
        System.out.println("=== GITHUB_DEBUG_ANALYZER_READY ===\n");

        log.info("=== ИНИЦИАЛИЗАЦИЯ PROCESSORS ЗАВЕРШЕНА ===");
    }
}