package ru.yandex.practicum.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.ScenarioWithDetails;
import ru.yandex.practicum.processor.HubRouterClientService;
import ru.yandex.practicum.repository.ScenarioRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class HandlerEventImpl implements HandlerEvent {
    private final ScenarioRepository scenarioRepository;
    private final HubRouterClientService hubRouterClientService;

    @Override
    public void handler(SensorsSnapshotAvro snapshotAvro) {
        final Map<String, SensorStateAvro> sensorStateAvroMap = snapshotAvro.getSensorsState();
        final List<ScenarioWithDetails> scenarios = scenarioRepository.findScenariosWithDetailsByHubId(snapshotAvro.getHubId());

        log.info("Обработка снапшота для хаба {}. Найдено сценариев: {}",
                snapshotAvro.getHubId(), scenarios.size());

        // Группируем сценарии по ID
        Map<Long, List<ScenarioWithDetails>> scenariosById = scenarios.stream()
                .collect(Collectors.groupingBy(ScenarioWithDetails::getScenarioId));

        for (Map.Entry<Long, List<ScenarioWithDetails>> entry : scenariosById.entrySet()) {
            Long scenarioId = entry.getKey();
            List<ScenarioWithDetails> scenarioDetails = entry.getValue();

            if (areAllConditionsMet(scenarioDetails, sensorStateAvroMap)) {
                String scenarioName = scenarioDetails.get(0).getScenarioName();
                log.info("Все условия сценария '{}' выполнены. Активация...", scenarioName);
                activateScenario(scenarioId, scenarioName, snapshotAvro.getHubId(), scenarioDetails);
            }
        }
    }

    private boolean areAllConditionsMet(List<ScenarioWithDetails> scenarioDetails,
                                        Map<String, SensorStateAvro> sensorStates) {
        // Получаем все уникальные условия для этого сценария
        List<ScenarioWithDetails> conditions = scenarioDetails.stream()
                .filter(detail -> detail.getConditionType() != null)
                .collect(Collectors.toList());

        if (conditions.isEmpty()) {
            return false;
        }

        // Проверяем каждое условие
        for (ScenarioWithDetails condition : conditions) {
            SensorStateAvro sensorState = sensorStates.get(condition.getSensorId());
            if (sensorState == null || !isConditionMet(condition, sensorState)) {
                return false;
            }
        }
        return true;
    }

    private boolean isConditionMet(ScenarioWithDetails condition, SensorStateAvro sensorState) {
        Object sensorData = sensorState.getData();

        switch (condition.getConditionType()) {
            case "TEMPERATURE":
                if (sensorData instanceof ClimateSensorAvro) {
                    ClimateSensorAvro climateSensor = (ClimateSensorAvro) sensorData;
                    return checkNumericCondition(condition, climateSensor.getTemperatureC());
                }
                break;

            case "HUMIDITY":
                if (sensorData instanceof ClimateSensorAvro) {
                    ClimateSensorAvro climateSensor = (ClimateSensorAvro) sensorData;
                    return checkNumericCondition(condition, climateSensor.getHumidity());
                }
                break;

            case "CO2LEVEL":
                if (sensorData instanceof ClimateSensorAvro) {
                    ClimateSensorAvro climateSensor = (ClimateSensorAvro) sensorData;
                    return checkNumericCondition(condition, climateSensor.getCo2Level());
                }
                break;

            case "MOTION":
                if (sensorData instanceof MotionSensorAvro) {
                    MotionSensorAvro motionSensor = (MotionSensorAvro) sensorData;
                    return checkBooleanCondition(condition, motionSensor.getMotion());
                }
                break;

            case "LUMINOSITY":
                if (sensorData instanceof LightSensorAvro) {
                    LightSensorAvro lightSensor = (LightSensorAvro) sensorData;
                    return checkNumericCondition(condition, lightSensor.getLuminosity());
                }
                break;

            case "SWITCH":
                if (sensorData instanceof SwitchSensorAvro) {
                    SwitchSensorAvro switchSensor = (SwitchSensorAvro) sensorData;
                    return checkBooleanCondition(condition, switchSensor.getStat());
                }
                break;
        }

        return false;
    }

    private boolean checkNumericCondition(ScenarioWithDetails condition, int sensorValue) {
        int conditionValue = condition.getConditionValue();

        switch (condition.getConditionOperation()) {
            case "GREATER_THAN":
                return sensorValue > conditionValue;
            case "LOWER_THAN":
                return sensorValue < conditionValue;
            case "EQUALS":
                return sensorValue == conditionValue;
            default:
                return false;
        }
    }

    private boolean checkBooleanCondition(ScenarioWithDetails condition, boolean sensorValue) {
        if ("EQUALS".equals(condition.getConditionOperation())) {
            boolean conditionValue = condition.getConditionValue() != 0;
            return sensorValue == conditionValue;
        }
        return false;
    }

    private void activateScenario(Long scenarioId, String scenarioName, String hubId,
                                  List<ScenarioWithDetails> scenarioDetails) {
        // Получаем все действия для этого сценария
        List<ScenarioWithDetails> actions = scenarioDetails.stream()
                .filter(detail -> detail.getActionType() != null && detail.getActionSensorId() != null)
                .distinct()
                .collect(Collectors.toList());

        if (actions.isEmpty()) {
            log.warn("Сценарий '{}' не содержит действий", scenarioName);
            return;
        }

        log.info("Выполняем {} действий для сценария '{}'", actions.size(), scenarioName);

        for (ScenarioWithDetails actionDetail : actions) {
            DeviceActionProto action = convertToDeviceActionProto(actionDetail);
            hubRouterClientService.sendDeviceAction(hubId, scenarioName, action);
        }
    }

    private DeviceActionProto convertToDeviceActionProto(ScenarioWithDetails actionDetail) {
        DeviceActionProto.Builder builder = DeviceActionProto.newBuilder()
                .setSensorId(actionDetail.getActionSensorId()) // Используем actionSensorId
                .setType(convertActionType(actionDetail.getActionType()));

        if (actionDetail.getActionValue() != null) {
            builder.setValue(actionDetail.getActionValue());
        }

        return builder.build();
    }

    private ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto convertActionType(String actionType) {
        switch (actionType.toUpperCase()) {
            case "ACTIVATE":
                return ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto.ACTIVATE;
            case "DEACTIVATE":
                return ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto.DEACTIVATE;
            case "INVERSE":
                return ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto.INVERSE;
            case "SET_VALUE":
                return ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto.SET_VALUE;
            default:
                throw new IllegalArgumentException("Unknown action type: " + actionType);
        }
    }
}