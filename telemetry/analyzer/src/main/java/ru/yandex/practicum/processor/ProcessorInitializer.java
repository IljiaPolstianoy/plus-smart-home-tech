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
        log.info("=== НАЧАЛО ИНИЦИАЛИЗАЦИИ PROCESSORS ===");

        try {
            // Ждем немного для старта всех сервисов
            Thread.sleep(5000);
            log.info("Ожидание завершено, запускаем процессоры...");

            // Запускаем процессоры
            hubEventProcessor.start();
            snapshotProcessor.start();

            // Ждем запуска потоков
            Thread.sleep(3000);

            // Активируем процессоры
            hubEventProcessor.setInitialized(true);
            snapshotProcessor.setInitialized(true);

            log.info("✅ Процессоры инициализированы и запущены");

        } catch (Exception e) {
            log.error("❌ Ошибка при инициализации процессоров", e);
            // Даже при ошибке пытаемся запустить процессоры
            hubEventProcessor.setInitialized(true);
            snapshotProcessor.setInitialized(true);
        }

        log.info("=== ИНИЦИАЛИЗАЦИЯ PROCESSORS ЗАВЕРШЕНА ===");
    }
}