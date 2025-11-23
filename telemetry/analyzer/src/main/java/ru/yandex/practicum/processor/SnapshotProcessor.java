package ru.yandex.practicum.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.handler.HandlerEvent;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor implements Runnable {
    private final KafkaConsumer<String, SensorsSnapshotAvro> snapshotConsumer;
    private final HandlerEvent handlerEvent;
    private volatile boolean running = true;
    private Thread processorThread;

    @Override
    public void run() {
        snapshotConsumer.subscribe(Collections.singletonList("telemetry.snapshots.v1"));
        log.info("SnapshotProcessor запущен и подписан на топик telemetry.snapshots.v1");

        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    ConsumerRecords<String, SensorsSnapshotAvro> records = snapshotConsumer.poll(Duration.ofMillis(1000));

                    for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                        System.out.println("=== GITHUB_DEBUG_SNAPSHOT ===");
                        System.out.println("📥 Снапшот получен: key=" + record.key() +
                                ", hubId=" + record.value().getHubId() +
                                ", сенсоров=" + record.value().getSensorsState().size());


                        SensorsSnapshotAvro snapshot = record.value();
                        log.info("📊 Снапшот детально: {}", snapshot);

                        // Логируем каждый сенсор в снапшоте
                        snapshot.getSensorsState().forEach((sensorId, sensorState) -> {
                            log.info("🔍 Сенсор {}: timestamp={}, data={}",
                                    sensorId, sensorState.getTimestamp(), sensorState.getData());
                        });

                        String hubId = snapshot.getHubId();
                        if (hubId == null) {
                            hubId = "default-hub";
                            log.warn("hubId был null, используем: {}", hubId);
                        }

                        handlerEvent.handler(record.value(), hubId);
                    }

                    if (!records.isEmpty()) {
                        snapshotConsumer.commitSync();
                        log.debug("Закоммичено {} сообщений", records.count());
                    }

                } catch (org.apache.kafka.common.errors.WakeupException e) {
                    // Это нормальное исключение при вызове wakeup()
                    log.info("WakeupException получен, завершаем работу SnapshotProcessor");
                    break;
                } catch (Exception e) {
                    log.error("Ошибка при обработке снапшотов", e);
                }
            }
        } finally {
            // Закрываем consumer без прерывания
            closeConsumerQuietly();
            log.info("SnapshotProcessor полностью остановлен");
        }
    }

    private void closeConsumerQuietly() {
        try {
            // Отписываемся от топиков перед закрытием
            snapshotConsumer.unsubscribe();
            // Закрываем consumer с небольшим таймаутом
            snapshotConsumer.close(Duration.ofSeconds(5));
        } catch (Exception e) {
            log.warn("Ошибка при закрытии consumer: {}", e.getMessage());
        }
    }

    public void start() {
        if (processorThread == null || !processorThread.isAlive()) {
            processorThread = new Thread(this, "SnapshotProcessorThread");
            processorThread.start();
            log.info("SnapshotProcessor запущен");
        }
    }

    public void shutdown() {
        log.info("Запущен graceful shutdown SnapshotProcessor");
        running = false;

        // Используем wakeup() для корректного выхода из poll()
        snapshotConsumer.wakeup();

        // Ждем завершения потока
        if (processorThread != null && processorThread.isAlive()) {
            try {
                processorThread.join(10000); // 10 секунд на завершение
                if (processorThread.isAlive()) {
                    log.warn("Поток SnapshotProcessor не завершился за отведенное время");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Прервано ожидание завершения SnapshotProcessor");
            }
        }
    }
}