package ru.yandex.practicum.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.handler.HandlerEvent;
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
        log.info("🎯 Начало обработки снапшота для хаба: {}", hubId);
        log.info("📊 Снапшот содержит сенсоры: {}", snapshotAvro.getSensorsState().keySet());

        // Детально логируем каждый сенсор в снапшоте
        snapshotAvro.getSensorsState().forEach((sensorId, sensorState) -> {
            log.info("🔍 Сенсор {}: timestamp={}, data={}",
                    sensorId, sensorState.getTimestamp(), sensorState.getData());
        });

        final Map<String, SensorStateAvro> sensorStateAvroMap = snapshotAvro.getSensorsState();

        final List<ScenarioProjection> scenarios = scenarioRepository.findScenariosWithDetailsByHubId(hubId);

        log.info("Обработка снапшота для хаба {}. Найдено записей: {}", hubId, scenarios.size());

        // Группируем по ID сценария
        Map<Long, List<ScenarioProjection>> scenariosById = scenarios.stream()
                .collect(Collectors.groupingBy(ScenarioProjection::getScenarioId));

        for (Map.Entry<Long, List<ScenarioProjection>> entry : scenariosById.entrySet()) {
            Long scenarioId = entry.getKey();
            List<ScenarioProjection> scenarioDetails = entry.getValue();

            String scenarioName = scenarioDetails.get(0).getScenarioName();
            log.info("🔍 Проверяем сценарий '{}' для хаба {}", scenarioName, hubId);

            // Детально логируем условия сценария
            List<ScenarioProjection> conditions = scenarioDetails.stream()
                    .filter(detail -> detail.getConditionType() != null)
                    .collect(Collectors.toList());

            log.info("   Условия сценария '{}':", scenarioName);
            for (ScenarioProjection condition : conditions) {
                log.info("     - Сенсор: {}, Тип: {}, Операция: {}, Значение: {}",
                        condition.getSensorId(), condition.getConditionType(),
                        condition.getConditionOperation(), condition.getConditionValue());
            }

            // Детально логируем действия сценария
            List<ScenarioProjection> actions = scenarioDetails.stream()
                    .filter(detail -> detail.getActionType() != null && detail.getActionSensorId() != null)
                    .collect(Collectors.toList());

            log.info("   Действия сценария '{}':", scenarioName);
            for (ScenarioProjection action : actions) {
                log.info("     - Сенсор: {}, Тип: {}, Значение: {}",
                        action.getActionSensorId(), action.getActionType(), action.getActionValue());
            }

            boolean allConditionsMet = areAllConditionsMet(scenarioDetails, sensorStateAvroMap);
            log.info("   Условия сценария '{}' выполнены: {}", scenarioName, allConditionsMet);

            if (allConditionsMet) {
                log.info("✅ АКТИВАЦИЯ СЦЕНАРИЯ '{}'", scenarioName);
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
            if (sensorState == null) {
                log.warn("❌ Сенсор {} не найден в снапшоте", condition.getSensorId());
                return false;
            }

            boolean conditionMet = isConditionMet(condition, sensorState);
            log.info("   Проверка условия для сенсора {}: {}", condition.getSensorId(), conditionMet);

            if (!conditionMet) {
                return false;
            }
        }
        return true;
    }

    private boolean isConditionMet(ScenarioProjection condition, SensorStateAvro sensorState) {
        Object sensorData = sensorState.getData();
        log.info("   Данные сенсора {}: {}", condition.getSensorId(), sensorData);

        switch (condition.getConditionType()) {
            case "TEMPERATURE":
                if (sensorData instanceof ClimateSensorAvro) {
                    ClimateSensorAvro climateSensor = (ClimateSensorAvro) sensorData;
                    log.info("   Температура: {}°C, условие: {} {}",
                            climateSensor.getTemperatureC(), condition.getConditionOperation(), condition.getConditionValue());
                    return checkNumericCondition(condition, climateSensor.getTemperatureC());
                }
                break;
            case "MOTION":
                if (sensorData instanceof MotionSensorAvro) {
                    MotionSensorAvro motionSensor = (MotionSensorAvro) sensorData;
                    log.info("   Движение: {}, условие: {} {}",
                            motionSensor.getMotion(), condition.getConditionOperation(), condition.getConditionValue());
                    return checkBooleanCondition(condition, motionSensor.getMotion());
                }
                break;
            case "SWITCH":
                if (sensorData instanceof SwitchSensorAvro) {
                    SwitchSensorAvro switchSensor = (SwitchSensorAvro) sensorData;
                    log.info("   Переключатель: {}, условие: {} {}",
                            switchSensor.getStat(), condition.getConditionOperation(), condition.getConditionValue());
                    return checkBooleanCondition(condition, switchSensor.getStat());
                }
                break;
            case "LUMINOSITY":
                if (sensorData instanceof LightSensorAvro) {
                    LightSensorAvro lightSensor = (LightSensorAvro) sensorData;
                    log.info("   Освещенность: {}, условие: {} {}",
                            lightSensor.getLuminosity(), condition.getConditionOperation(), condition.getConditionValue());
                    return checkNumericCondition(condition, lightSensor.getLuminosity());
                }
                break;
            case "HUMIDITY":
            case "CO2LEVEL":
                if (sensorData instanceof ClimateSensorAvro) {
                    ClimateSensorAvro climateSensor = (ClimateSensorAvro) sensorData;
                    int value = "HUMIDITY".equals(condition.getConditionType()) ?
                            climateSensor.getHumidity() : climateSensor.getCo2Level();
                    log.info("   {}: {}, условие: {} {}",
                            condition.getConditionType(), value, condition.getConditionOperation(), condition.getConditionValue());
                    return checkNumericCondition(condition, value);
                }
                break;
            default:
                log.warn("❌ Неизвестный тип условия: {}", condition.getConditionType());
        }
        return false;
    }

    private boolean checkNumericCondition(ScenarioProjection condition, int sensorValue) {
        Integer conditionValue = condition.getConditionValue();
        if (conditionValue == null) {
            log.warn("❌ Отсутствует значение условия для числовой проверки");
            return false;
        }

        boolean result;
        switch (condition.getConditionOperation()) {
            case "GREATER_THAN":
                result = sensorValue > conditionValue;
                log.info("   {} > {} = {}", sensorValue, conditionValue, result);
                return result;
            case "LOWER_THAN":
                result = sensorValue < conditionValue;
                log.info("   {} < {} = {}", sensorValue, conditionValue, result);
                return result;
            case "EQUALS":
                result = sensorValue == conditionValue;
                log.info("   {} == {} = {}", sensorValue, conditionValue, result);
                return result;
            default:
                log.warn("❌ Неизвестная операция: {}", condition.getConditionOperation());
                return false;
        }
    }

    private boolean checkBooleanCondition(ScenarioProjection condition, boolean sensorValue) {
        if ("EQUALS".equals(condition.getConditionOperation())) {
            Integer conditionValue = condition.getConditionValue();
            boolean conditionBool = conditionValue != null && conditionValue != 0;
            boolean result = sensorValue == conditionBool;
            log.info("   {} == {} = {}", sensorValue, conditionBool, result);
            return result;
        }
        log.warn("❌ Неподдерживаемая операция для boolean: {}", condition.getConditionOperation());
        return false;
    }

    private void activateScenario(Long scenarioId, String scenarioName, String hubId,
                                  List<ScenarioProjection> scenarioDetails) {
        // Фильтруем только действия (где есть actionType)
        List<ScenarioProjection> actions = scenarioDetails.stream()
                .filter(detail -> detail.getActionType() != null && detail.getActionSensorId() != null)
                .collect(Collectors.toList());

        log.info("🎯 Выполняем {} действий для сценария '{}'", actions.size(), scenarioName);

        for (ScenarioProjection actionDetail : actions) {
            log.info("   🚀 Действие: сенсор={}, тип={}, значение={}",
                    actionDetail.getActionSensorId(), actionDetail.getActionType(), actionDetail.getActionValue());

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