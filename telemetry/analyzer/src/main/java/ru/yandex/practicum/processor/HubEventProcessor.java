package ru.yandex.practicum.processor;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.model.*;
import ru.yandex.practicum.repository.*;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {
    @Autowired
    private ConsumerFactory<String, HubEventAvro> hubEventConsumerFactory;

    private KafkaConsumer<String, HubEventAvro> hubEventConsumer;

    @PostConstruct
    public void init() {
        this.hubEventConsumer = (KafkaConsumer<String, HubEventAvro>) hubEventConsumerFactory.createConsumer();
    }

    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;

    @Override
    public void run() {
        hubEventConsumer.subscribe(Collections.singletonList("telemetry.hubs.v1"));
        log.info("HubEventProcessor запущен и подписан на топик telemetry.hubs.v1");

        while (!Thread.currentThread().isInterrupted()) {
            try {
                ConsumerRecords<String, HubEventAvro> records = hubEventConsumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    log.info("Получено hub событие для хаба: {}", record.key());
                    processHubEvent(record.value());
                }

                hubEventConsumer.commitSync();
            } catch (Exception e) {
                log.error("Ошибка при обработке hub events", e);
            }
        }

        hubEventConsumer.close();
        log.info("HubEventProcessor остановлен");
    }

    private void processHubEvent(HubEventAvro hubEvent) {
        System.out.println("=== GITHUB_DEBUG_HUB_EVENT ===");
        System.out.println("📥 Hub событие: " + hubEvent.getPayload().getClass().getSimpleName() +
                ", хаб: " + hubEvent.getHubId());
        try {
            log.info("📥 Получено hub событие: {}", hubEvent); // Весь объект
            String hubId = hubEvent.getHubId();
            log.info("Hub ID: {}, Timestamp: {}, Payload type: {}",
                    hubId, hubEvent.getTimestamp(), hubEvent.getPayload().getClass().getSimpleName());

            switch (hubEvent.getPayload().getClass().getSimpleName()) {
                case "DeviceAddedEventAvro":
                    processDeviceAdded(hubId, (ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro) hubEvent.getPayload());
                    break;
                case "DeviceRemovedEventAvro":
                    processDeviceRemoved(hubId, (ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro) hubEvent.getPayload());
                    break;
                case "ScenarioAddedEventAvro":
                    processScenarioAdded(hubId, (ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro) hubEvent.getPayload());
                    break;
                case "ScenarioRemovedEventAvro":    System.out.println("📥 Hub событие: " + hubEvent.getPayload().getClass().getSimpleName() +
                        ", хаб: " + hubEvent.getHubId());
                    processScenarioRemoved(hubId, (ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro) hubEvent.getPayload());
                    break;
                default:
                    log.warn("Неизвестный тип события: {}", hubEvent.getPayload().getClass().getSimpleName());
            }
        } catch (Exception e) {
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

        // Удаляем связи сценариев с этим датчиком
        scenarioConditionRepository.deleteBySensorId(deviceId);
        scenarioActionRepository.deleteBySensorId(deviceId);

        // Удаляем сам датчик
        sensorRepository.deleteById(deviceId);
        log.info("Удален датчик {} из хаба {}", deviceId, hubId);
    }

    private void processScenarioAdded(String hubId, ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro scenarioAdded) {
        String scenarioName = scenarioAdded.getName();

        // Проверяем существование сценария
        Optional<Scenario> existingScenario = scenarioRepository.findByHubIdAndName(hubId, scenarioName);
        if (existingScenario.isPresent()) {
            log.info("Сценарий '{}' уже существует в хабе {}. Обновляем...", scenarioName, hubId);
            // Удаляем старый сценарий и создаем новый
            processScenarioRemoved(hubId,
                    ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro.newBuilder()
                            .setName(scenarioName)
                            .build());
        }

        // Создаем новый сценарий
        Scenario scenario = new Scenario();
        scenario.setHubId(hubId);
        scenario.setName(scenarioName);
        Scenario savedScenario = scenarioRepository.save(scenario);
        scenarioRepository.flush(); // Обновляем сессию

        // Обрабатываем условия
        processScenarioConditions(savedScenario, scenarioAdded.getConditions());

        // Обрабатываем действия
        processScenarioActions(savedScenario, scenarioAdded.getActions());

        log.info("Добавлен сценарий '{}' в хаб {} с {} условиями и {} действиями",
                scenarioName, hubId, scenarioAdded.getConditions().size(), scenarioAdded.getActions().size());
    }

    private void processScenarioConditions(Scenario scenario, java.util.List<ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro> conditions) {
        for (ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro conditionAvro : conditions) {
            // Создаем условие
            Condition condition = new Condition();
            condition.setType(conditionAvro.getType().toString());
            condition.setOperation(conditionAvro.getOperation().toString());

            // Обрабатываем значение условия
            Object conditionValue = conditionAvro.getValue();
            if (conditionValue instanceof Boolean) {
                condition.setValue(((Boolean) conditionValue) ? 1 : 0);
            } else if (conditionValue instanceof Integer) {
                condition.setValue((Integer) conditionValue);
            } else {
                log.warn("Неизвестный тип значения условия: {}", conditionValue.getClass().getSimpleName());
                continue;
            }

            // Сохраняем условие
            Condition savedCondition = conditionRepository.save(condition);
            conditionRepository.flush(); // Обновляем сессию

            // Проверяем существование датчика
            String sensorId = conditionAvro.getSensorId();
            Optional<Sensor> sensorOpt = sensorRepository.findById(sensorId);
            if (sensorOpt.isEmpty()) {
                log.warn("Датчик {} не найден, создаем...", sensorId);
                Sensor sensor = new Sensor();
                sensor.setId(sensorId);
                sensor.setHubId(scenario.getHubId());
                sensorRepository.save(sensor);
                sensorRepository.flush(); // Обновляем сессию
                sensorOpt = Optional.of(sensor);
            }

            // Создаем связь сценарий-датчик-условие
            ScenarioCondition scenarioCondition = new ScenarioCondition();
            scenarioCondition.setScenario(scenario);
            scenarioCondition.setSensorId(sensorOpt.get().getId());
            scenarioCondition.setCondition(savedCondition);

            scenarioConditionRepository.save(scenarioCondition);
        }
    }

    private void processScenarioActions(Scenario scenario, java.util.List<ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro> actions) {
        for (ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro actionAvro : actions) {
            // Создаем действие
            Action action = new Action();
            action.setType(actionAvro.getType().toString());

            if (actionAvro.getValue() != null) {
                action.setValue(actionAvro.getValue());
            }

            // Сохраняем действие
            Action savedAction = actionRepository.save(action);
            actionRepository.flush(); // Обновляем сессию

            // Проверяем существование датчика
            String sensorId = actionAvro.getSensorId();
            Optional<Sensor> sensorOpt = sensorRepository.findById(sensorId);
            if (sensorOpt.isEmpty()) {
                log.warn("Датчик {} не найден, создаем...", sensorId);
                Sensor sensor = new Sensor();
                sensor.setId(sensorId);
                sensor.setHubId(scenario.getHubId());
                sensorRepository.save(sensor);
                sensorRepository.flush(); // Обновляем сессию
                sensorOpt = Optional.of(sensor);
            }

            // Создаем связь сценарий-датчик-действие
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

        // Удаляем связи условий
        scenarioConditionRepository.deleteByScenarioId(scenario.getId());

        // Удаляем связи действий
        scenarioActionRepository.deleteByScenarioId(scenario.getId());

        // Удаляем сам сценарий
        scenarioRepository.delete(scenario);

        log.info("Удален сценарий '{}' из хаба {}", scenarioName, hubId);
    }
}