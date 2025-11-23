package ru.yandex.practicum.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.model.ScenarioProjection;
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
    public void handler(SensorsSnapshotAvro snapshotAvro, String hubId) {
        final Map<String, SensorStateAvro> sensorStateAvroMap = snapshotAvro.getSensorsState();

        final List<ScenarioProjection> scenarios = scenarioRepository.findScenariosWithDetailsByHubId(hubId);

        log.info("Обработка снапшота для хаба {}. Найдено записей: {}", hubId, scenarios.size());

        // Группируем по ID сценария
        Map<Long, List<ScenarioProjection>> scenariosById = scenarios.stream()
                .collect(Collectors.groupingBy(ScenarioProjection::getScenarioId));

        for (Map.Entry<Long, List<ScenarioProjection>> entry : scenariosById.entrySet()) {
            Long scenarioId = entry.getKey();
            List<ScenarioProjection> scenarioDetails = entry.getValue();

            if (areAllConditionsMet(scenarioDetails, sensorStateAvroMap)) {
                String scenarioName = scenarioDetails.get(0).getScenarioName();
                log.info("Все условия сценария '{}' выполнены. Активация...", scenarioName);
                activateScenario(scenarioId, scenarioName, hubId, scenarioDetails);
            }
        }
    }

    private boolean areAllConditionsMet(List<ScenarioProjection> scenarioDetails,
                                        Map<String, SensorStateAvro> sensorStates) {
        // Фильтруем только условия (где есть conditionType)
        List<ScenarioProjection> conditions = scenarioDetails.stream()
                .filter(detail -> detail.getConditionType() != null)
                .collect(Collectors.toList());

        if (conditions.isEmpty()) {
            log.warn("Нет условий для проверки");
            return false;
        }

        for (ScenarioProjection condition : conditions) {
            SensorStateAvro sensorState = sensorStates.get(condition.getSensorId());
            if (sensorState == null || !isConditionMet(condition, sensorState)) {
                return false;
            }
        }
        return true;
    }

    private boolean isConditionMet(ScenarioProjection condition, SensorStateAvro sensorState) {
        Object sensorData = sensorState.getData();

        switch (condition.getConditionType()) {
            case "TEMPERATURE":
                if (sensorData instanceof ClimateSensorAvro) {
                    ClimateSensorAvro climateSensor = (ClimateSensorAvro) sensorData;
                    return checkNumericCondition(condition, climateSensor.getTemperatureC());
                }
                break;
            case "MOTION":
                if (sensorData instanceof MotionSensorAvro) {
                    MotionSensorAvro motionSensor = (MotionSensorAvro) sensorData;
                    return checkBooleanCondition(condition, motionSensor.getMotion());
                }
                break;
            case "SWITCH":
                if (sensorData instanceof SwitchSensorAvro) {
                    SwitchSensorAvro switchSensor = (SwitchSensorAvro) sensorData;
                    return checkBooleanCondition(condition, switchSensor.getStat());
                }
                break;
            // ... остальные типы
        }
        return false;
    }

    private boolean checkNumericCondition(ScenarioProjection condition, int sensorValue) {
        Integer conditionValue = condition.getConditionValue();
        if (conditionValue == null) return false;

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

    private boolean checkBooleanCondition(ScenarioProjection condition, boolean sensorValue) {
        if ("EQUALS".equals(condition.getConditionOperation())) {
            Integer conditionValue = condition.getConditionValue();
            boolean conditionBool = conditionValue != null && conditionValue != 0;
            return sensorValue == conditionBool;
        }
        return false;
    }

    private void activateScenario(Long scenarioId, String scenarioName, String hubId,
                                  List<ScenarioProjection> scenarioDetails) {
        // Фильтруем только действия (где есть actionType)
        List<ScenarioProjection> actions = scenarioDetails.stream()
                .filter(detail -> detail.getActionType() != null && detail.getActionSensorId() != null)
                .collect(Collectors.toList());

        log.info("Выполняем {} действий для сценария '{}'", actions.size(), scenarioName);

        for (ScenarioProjection actionDetail : actions) {
            DeviceActionProto action = convertToDeviceActionProto(actionDetail);
            hubRouterClientService.sendDeviceAction(hubId, scenarioName, action);
        }
    }

    private DeviceActionProto convertToDeviceActionProto(ScenarioProjection actionDetail) {
        DeviceActionProto.Builder builder = DeviceActionProto.newBuilder()
                .setSensorId(actionDetail.getActionSensorId())
                .setType(convertActionType(actionDetail.getActionType()));

        if (actionDetail.getActionValue() != null) {
            builder.setValue(actionDetail.getActionValue());
        }

        return builder.build();
    }

    private ActionTypeProto convertActionType(String actionType) {
        switch (actionType.toUpperCase()) {
            case "ACTIVATE":
                return ActionTypeProto.ACTIVATE;
            case "DEACTIVATE":
                return ActionTypeProto.DEACTIVATE;
            case "INVERSE":
                return ActionTypeProto.INVERSE;
            case "SET_VALUE":
                return ActionTypeProto.SET_VALUE;
            default:
                throw new IllegalArgumentException("Unknown action type: " + actionType);
        }
    }
}