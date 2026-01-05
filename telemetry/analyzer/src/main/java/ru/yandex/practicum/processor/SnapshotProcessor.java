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
        System.out.println("\n\n=== GITHUB_DEBUG_SNAPSHOT_PROCESSOR_START ===");
        System.out.println("🚀 SnapshotProcessor ЗАПУЩЕН!");
        System.out.println("Thread: " + Thread.currentThread().getName());
        System.out.println("HandlerEvent: " + (handlerEvent != null ? "OK" : "NULL"));
        System.out.println("Initialized: " + initialized.get());

        // Ждем инициализации перед стартом обработки
        waitForInitialization();

        if (!initialized.get()) {
            System.out.println("❌ SnapshotProcessor НЕ ИНИЦИАЛИЗИРОВАН!");
            return;
        }

        System.out.println("✅ SnapshotProcessor инициализирован, подписываемся на Kafka...");

        snapshotConsumer.subscribe(Collections.singletonList("telemetry.snapshots.v1"));
        System.out.println("✅ Подписались на топик telemetry.snapshots.v1");

        int messageCount = 0;

        try {
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    System.out.println("⏳ Ожидаем сообщения из Kafka...");
                    ConsumerRecords<String, SensorsSnapshotAvro> records =
                            snapshotConsumer.poll(Duration.ofMillis(pollTimeout));

                    if (!records.isEmpty()) {
                        messageCount += records.count();
                        System.out.println("📥 Получено " + records.count() + " сообщений (всего: " + messageCount + ")");

                        for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                            System.out.println("=== GITHUB_DEBUG_SNAPSHOT_RECEIVED ===");
                            System.out.println("Key: " + record.key());
                            System.out.println("HubId: " + record.value().getHubId());
                            System.out.println("Partition: " + record.partition());
                            System.out.println("Offset: " + record.offset());

                            SensorsSnapshotAvro snapshot = record.value();
                            String hubId = snapshot.getHubId();
                            if (hubId == null) {
                                hubId = "default-hub";
                            }

                            System.out.println("🚀 ВЫЗЫВАЕМ handler для хаба: " + hubId);
                            try {
                                handlerEvent.handler(record.value(), hubId);
                                System.out.println("✅ handler успешно вызван");
                            } catch (Exception e) {
                                System.out.println("❌ Ошибка в handler: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }

                        snapshotConsumer.commitSync();
                        System.out.println("✅ Коммит сделан");
                    } else {
                        System.out.println("⏳ Сообщений нет...");
                    }

                } catch (org.apache.kafka.common.errors.WakeupException e) {
                    System.out.println("🛑 WakeupException - завершаем работу");
                    break;
                } catch (Exception e) {
                    System.out.println("❌ Ошибка в SnapshotProcessor: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } finally {
            System.out.println("=== GITHUB_DEBUG_SNAPSHOT_PROCESSOR_STOP ===");
            System.out.println("Всего обработано сообщений: " + messageCount);
            closeConsumerQuietly();
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