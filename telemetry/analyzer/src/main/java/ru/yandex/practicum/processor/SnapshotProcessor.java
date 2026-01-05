package ru.yandex.practicum.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.handler.HandlerEvent;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor implements Runnable {
    private final KafkaConsumer<String, SensorsSnapshotAvro> snapshotConsumer;
    private final HandlerEvent handlerEvent;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private Thread processorThread;

    @Value("${processor.initialization.timeout:15000}")
    private long initializationTimeout;

    @Value("${processor.poll.timeout:1000}")
    private long pollTimeout;

    @Override
    public void run() {
        log.info("SnapshotProcessor: запуск потока, ждем инициализации...");

        // Ждем инициализации
        waitForInitialization();

        if (!initialized.get()) {
            log.error("SnapshotProcessor: не инициализирован за {} мс, завершаем поток", initializationTimeout);
            return;
        }

        log.info("SnapshotProcessor: подписываемся на топик telemetry.snapshots.v1");
        snapshotConsumer.subscribe(Collections.singletonList("telemetry.snapshots.v1"));

        System.out.println("=== GITHUB_DEBUG_SNAPSHOT_PROCESSOR_START ===");
        System.out.println("✅ SnapshotProcessor запущен и подписан на топик");

        try {
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    ConsumerRecords<String, SensorsSnapshotAvro> records =
                            snapshotConsumer.poll(Duration.ofMillis(pollTimeout));

                    System.out.println("📊 Получено записей: " + records.count());

                    if (!records.isEmpty()) {
                        System.out.println("🎯 Начинаем обработку " + records.count() + " снапшотов");
                    }

                    for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                        System.out.println("=== GITHUB_DEBUG_SNAPSHOT_RECEIVED ===");
                        System.out.println("📥 Снапшот получен:");
                        System.out.println("  Key: " + record.key());
                        System.out.println("  HubId: " + record.value().getHubId());
                        System.out.println("  Сенсоров: " + record.value().getSensorsState().size());

                        // Проверяем handler
                        if (handlerEvent == null) {
                            System.out.println("❌ CRITICAL: handlerEvent is NULL!");
                        } else {
                            System.out.println("✅ HandlerEvent доступен");
                        }

                        SensorsSnapshotAvro snapshot = record.value();
                        String hubId = snapshot.getHubId();
                        if (hubId == null) {
                            hubId = "default-hub";
                            log.warn("hubId был null, используем: {}", hubId);
                        }

                        System.out.println("🚀 Вызываем handler для хаба: " + hubId);
                        handlerEvent.handler(record.value(), hubId);
                        System.out.println("✅ Handler вызван");
                    }

                    if (!records.isEmpty()) {
                        snapshotConsumer.commitSync();
                    }

                } catch (org.apache.kafka.common.errors.WakeupException e) {
                    log.info("WakeupException получен, завершаем работу SnapshotProcessor");
                    break;
                } catch (Exception e) {
                    log.error("Ошибка при обработке снапшотов", e);
                    System.out.println("❌ Ошибка обработки снапшота: " + e.getMessage());
                }
            }
        } finally {
            System.out.println("=== GITHUB_DEBUG_SNAPSHOT_PROCESSOR_STOP ===");
            closeConsumerQuietly();
            log.info("SnapshotProcessor полностью остановлен");
        }
    }

    private void waitForInitialization() {
        long startTime = System.currentTimeMillis();
        log.info("SnapshotProcessor: ожидание инициализации (таймаут: {} мс)", initializationTimeout);

        while (!initialized.get() &&
                (System.currentTimeMillis() - startTime) < initializationTimeout) {
            try {
                Thread.sleep(1000);
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                log.info("SnapshotProcessor: ждем инициализации... {} сек", elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("SnapshotProcessor: прервано ожидание инициализации");
                break;
            }
        }

        if (initialized.get()) {
            log.info("SnapshotProcessor: успешно инициализирован за {} мс",
                    System.currentTimeMillis() - startTime);
        } else {
            log.warn("SnapshotProcessor: не инициализирован в течение {} мс",
                    System.currentTimeMillis() - startTime);
        }
    }

    private void closeConsumerQuietly() {
        try {
            snapshotConsumer.unsubscribe();
            snapshotConsumer.close(Duration.ofSeconds(5));
        } catch (Exception e) {
            log.warn("Ошибка при закрытии consumer: {}", e.getMessage());
        }
    }

    public void start() {
        if (processorThread == null || !processorThread.isAlive()) {
            processorThread = new Thread(this, "SnapshotProcessorThread");
            processorThread.start();
            log.info("SnapshotProcessor поток запущен (ожидает инициализации)");
        }
    }

    public void shutdown() {
        log.info("Запущен graceful shutdown SnapshotProcessor");
        running.set(false);
        snapshotConsumer.wakeup();

        if (processorThread != null && processorThread.isAlive()) {
            try {
                processorThread.join(10000);
                if (processorThread.isAlive()) {
                    log.warn("Поток SnapshotProcessor не завершился за отведенное время");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Прервано ожидание завершения SnapshotProcessor");
            }
        }
    }

    public void setInitialized(boolean value) {
        initialized.set(value);
        log.info("SnapshotProcessor initialized = {}", value);
    }

    public boolean isInitialized() {
        return initialized.get();
    }
}