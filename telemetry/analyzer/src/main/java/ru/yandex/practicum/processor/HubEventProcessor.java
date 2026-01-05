package ru.yandex.practicum.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.repository.*;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {
    private final KafkaConsumer<String, HubEventAvro> hubEventConsumer;
    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private Thread processorThread;

    @Value("${processor.initialization.timeout:15000}")
    private long initializationTimeout;

    @Override
    public void run() {
        System.out.println("\n\n=== GITHUB_DEBUG_HUB_EVENT_PROCESSOR_START ===");
        System.out.println("🚀 HubEventProcessor ЗАПУЩЕН!");
        System.out.println("Thread: " + Thread.currentThread().getName());
        System.out.println("Initialized: " + initialized.get());

        waitForInitialization();

        if (!initialized.get()) {
            System.out.println("❌ HubEventProcessor НЕ ИНИЦИАЛИЗИРОВАН!");
            return;
        }

        System.out.println("✅ HubEventProcessor инициализирован, подписываемся на Kafka...");

        hubEventConsumer.subscribe(Collections.singletonList("telemetry.hubs.v1"));
        System.out.println("✅ Подписались на топик telemetry.hubs.v1");

        int messageCount = 0;

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                System.out.println("⏳ Ожидаем hub событий из Kafka...");
                ConsumerRecords<String, HubEventAvro> records =
                        hubEventConsumer.poll(Duration.ofMillis(1000));

                if (!records.isEmpty()) {
                    messageCount += records.count();
                    System.out.println("📥 Получено " + records.count() + " hub событий (всего: " + messageCount + ")");

                    for (ConsumerRecord<String, HubEventAvro> record : records) {
                        System.out.println("=== GITHUB_DEBUG_HUB_EVENT_RECEIVED ===");
                        System.out.println("Key: " + record.key());
                        System.out.println("Payload type: " + record.value().getPayload().getClass().getSimpleName());

                        try {
                            processHubEvent(record.value());
                            System.out.println("✅ Hub событие обработано");
                        } catch (Exception e) {
                            System.out.println("❌ Ошибка обработки hub события: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    hubEventConsumer.commitSync();
                }

            } catch (Exception e) {
                System.out.println("❌ Ошибка в HubEventProcessor: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("=== GITHUB_DEBUG_HUB_EVENT_PROCESSOR_STOP ===");
        System.out.println("Всего обработано hub событий: " + messageCount);
        hubEventConsumer.close();
    }

    private void waitForInitialization() {
        long startTime = System.currentTimeMillis();
        log.info("HubEventProcessor: ожидание инициализации (таймаут: {} мс)", initializationTimeout);

        while (!initialized.get() &&
                (System.currentTimeMillis() - startTime) < initializationTimeout) {
            try {
                Thread.sleep(1000);
                long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                log.info("HubEventProcessor: ждем инициализации... {} сек", elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("HubEventProcessor: прервано ожидание инициализации");
                break;
            }
        }

        if (initialized.get()) {
            log.info("HubEventProcessor: успешно инициализирован за {} мс",
                    System.currentTimeMillis() - startTime);
        } else {
            log.warn("HubEventProcessor: не инициализирован в течение {} мс",
                    System.currentTimeMillis() - startTime);
        }
    }

    private void processHubEvent(HubEventAvro hubEvent) {
        System.out.println("\n=== GITHUB_DEBUG_HUB_EVENT_RECEIVED ===");
        System.out.println("📥 Hub событие получено:");
        System.out.println("  Hub ID: " + hubEvent.getHubId());
        System.out.println("  Timestamp: " + hubEvent.getTimestamp());
        System.out.println("  Payload type: " + hubEvent.getPayload().getClass().getSimpleName());

        try {
            String hubId = hubEvent.getHubId();
            log.info("📥 Получено hub событие: {}", hubEvent);

            switch (hubEvent.getPayload().getClass().getSimpleName()) {
                case "DeviceAddedEventAvro":
                    System.out.println("  Тип: DEVICE_ADDED");
                    processDeviceAdded(hubId, (ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro) hubEvent.getPayload());
                    break;
                case "DeviceRemovedEventAvro":
                    System.out.println("  Тип: DEVICE_REMOVED");
                    processDeviceRemoved(hubId, (ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro) hubEvent.getPayload());
                    break;
                case "ScenarioAddedEventAvro":
                    System.out.println("  Тип: SCENARIO_ADDED");
                    processScenarioAdded(hubId, (ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro) hubEvent.getPayload());
                    break;
                case "ScenarioRemovedEventAvro":
                    System.out.println("  Тип: SCENARIO_REMOVED");
                    processScenarioRemoved(hubId, (ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro) hubEvent.getPayload());
                    break;
                default:
                    System.out.println("  ⚠️ Неизвестный тип события: " + hubEvent.getPayload().getClass().getSimpleName());
                    log.warn("Неизвестный тип события: {}", hubEvent.getPayload().getClass().getSimpleName());
            }

            System.out.println("✅ Hub событие обработано");
        } catch (Exception e) {
            System.out.println("❌ Ошибка обработки hub события: " + e.getMessage());
            log.error("❌ Ошибка обработки hub события: {}", hubEvent, e);
        }
    }

    private void processDeviceAdded(String hubId, ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro deviceAdded) {
        String deviceId = deviceAdded.getId();

        if (sensorRepository.existsById(deviceId)) {
            log.info("Датчик {} уже существует в хабе {}", deviceId, hubId);
            return;
        }

        Sensor sensor = new Sensor();
        sensor.setId(deviceId);
        sensor.setHubId(hubId);

        sensorRepository.save(sensor);
        log.info("Добавлен датчик {} в хаб {}", deviceId, hubId);
    }

    private void processDeviceRemoved(String hubId, ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro deviceRemoved) {
        String deviceId = deviceRemoved.getId();

        Optional<Sensor> sensorOpt = sensorRepository.findById(deviceId);
        if (sensorOpt.isEmpty()) {
            log.info("Датчик {} не найден в хабе {}", deviceId, hubId);
            return;
        }

        scenarioConditionRepository.deleteBySensorId(deviceId);
        scenarioActionRepository.deleteBySensorId(deviceId);

        sensorRepository.deleteById(deviceId);
        log.info("Удален датчик {} из хаба {}", deviceId, hubId);
    }

    private void processScenarioAdded(String hubId, ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro scenarioAdded) {
        String scenarioName = scenarioAdded.getName();

        log.info("🔄 Обработка добавления сценария '{}' для хаба {}", scenarioName, hubId);

        // Удаляем старый сценарий если есть
        Optional<Scenario> existingScenario = scenarioRepository.findByHubIdAndName(hubId, scenarioName);
        if (existingScenario.isPresent()) {
            log.info("Сценарий '{}' уже существует в хабе {}. Обновляем...", scenarioName, hubId);
            scenarioConditionRepository.deleteByScenarioId(existingScenario.get().getId());
            scenarioActionRepository.deleteByScenarioId(existingScenario.get().getId());
            scenarioRepository.delete(existingScenario.get());
            scenarioRepository.flush();
        }

        // Создаем новый сценарий
        Scenario scenario = new Scenario();
        scenario.setHubId(hubId);
        scenario.setName(scenarioName);
        Scenario savedScenario = scenarioRepository.save(scenario);
        scenarioRepository.flush();

        // Обрабатываем условия
        if (scenarioAdded.getConditions() != null) {
            processScenarioConditions(savedScenario, scenarioAdded.getConditions());
        }

        // Обрабатываем действия
        if (scenarioAdded.getActions() != null) {
            processScenarioActions(savedScenario, scenarioAdded.getActions());
        }

        log.info("✅ Добавлен сценарий '{}' в хаб {} с {} условиями и {} действиями",
                scenarioName, hubId,
                scenarioAdded.getConditions() != null ? scenarioAdded.getConditions().size() : 0,
                scenarioAdded.getActions() != null ? scenarioAdded.getActions().size() : 0);
    }

    private void processScenarioConditions(Scenario scenario, java.util.List<ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro> conditions) {
        for (ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro conditionAvro : conditions) {
            Condition condition = new Condition();
            condition.setType(conditionAvro.getType().toString());
            condition.setOperation(conditionAvro.getOperation().toString());

            Object conditionValue = conditionAvro.getValue();
            if (conditionValue instanceof Boolean) {
                condition.setValue(((Boolean) conditionValue) ? 1 : 0);
            } else if (conditionValue instanceof Integer) {
                condition.setValue((Integer) conditionValue);
            } else {
                log.warn("Неизвестный тип значения условия: {}", conditionValue.getClass().getSimpleName());
                continue;
            }

            Condition savedCondition = conditionRepository.save(condition);
            conditionRepository.flush();

            String sensorId = conditionAvro.getSensorId();
            Optional<Sensor> sensorOpt = sensorRepository.findById(sensorId);
            if (sensorOpt.isEmpty()) {
                log.warn("Датчик {} не найден, создаем...", sensorId);
                Sensor sensor = new Sensor();
                sensor.setId(sensorId);
                sensor.setHubId(scenario.getHubId());
                sensorRepository.save(sensor);
                sensorRepository.flush();
                sensorOpt = Optional.of(sensor);
            }

            ScenarioCondition scenarioCondition = new ScenarioCondition();
            scenarioCondition.setScenario(scenario);
            scenarioCondition.setSensorId(sensorOpt.get().getId());
            scenarioCondition.setCondition(savedCondition);

            scenarioConditionRepository.save(scenarioCondition);
        }
    }

    private void processScenarioActions(Scenario scenario, java.util.List<ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro> actions) {
        for (ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro actionAvro : actions) {
            Action action = new Action();
            action.setType(actionAvro.getType().toString());

            if (actionAvro.getValue() != null) {
                action.setValue(actionAvro.getValue());
            }

            Action savedAction = actionRepository.save(action);
            actionRepository.flush();

            String sensorId = actionAvro.getSensorId();
            Optional<Sensor> sensorOpt = sensorRepository.findById(sensorId);
            if (sensorOpt.isEmpty()) {
                log.warn("Датчик {} не найден, создаем...", sensorId);
                Sensor sensor = new Sensor();
                sensor.setId(sensorId);
                sensor.setHubId(scenario.getHubId());
                sensorRepository.save(sensor);
                sensorRepository.flush();
                sensorOpt = Optional.of(sensor);
            }

            ScenarioAction scenarioAction = new ScenarioAction();
            scenarioAction.setScenario(scenario);
            scenarioAction.setSensorId(sensorOpt.get().getId());
            scenarioAction.setAction(savedAction);

            scenarioActionRepository.save(scenarioAction);
        }
    }

    private void processScenarioRemoved(String hubId, ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro scenarioRemoved) {
        String scenarioName = scenarioRemoved.getName();

        Optional<Scenario> scenarioOpt = scenarioRepository.findByHubIdAndName(hubId, scenarioName);
        if (scenarioOpt.isEmpty()) {
            log.info("Сценарий '{}' не найден в хабе {}", scenarioName, hubId);
            return;
        }

        Scenario scenario = scenarioOpt.get();

        scenarioConditionRepository.deleteByScenarioId(scenario.getId());
        scenarioActionRepository.deleteByScenarioId(scenario.getId());
        scenarioRepository.delete(scenario);

        log.info("Удален сценарий '{}' из хаба {}", scenarioName, hubId);
    }

    public void start() {
        if (processorThread == null || !processorThread.isAlive()) {
            processorThread = new Thread(this, "HubEventProcessorThread");
            processorThread.start();
            log.info("HubEventProcessor поток запущен (ожидает инициализации)");
        }
    }

    public void shutdown() {
        running.set(false);
        hubEventConsumer.wakeup();
        log.info("HubEventProcessor shutdown initiated");
    }

    public void setInitialized(boolean value) {
        initialized.set(value);
        log.info("HubEventProcessor initialized = {}", value);
    }

    public boolean isInitialized() {
        return initialized.get();
    }
}