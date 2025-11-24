package ru.yandex.practicum.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.generic.GenericRecord;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.model.ScenarioProjection;
import ru.yandex.practicum.processor.HubRouterClientService;
import ru.yandex.practicum.repository.ScenarioRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class HandlerEventImpl implements HandlerEvent {

    private static final String[] TEMPERATURE_FIELDS = {"temperatureC", "temperature", "temp", "value"};
    private static final String FIELD_LUMINOSITY = "luminosity";
    private static final String FIELD_MOTION = "motion";
    private static final String FIELD_SWITCH = "stat";

    private final ScenarioRepository scenarioRepository;
    private final HubRouterClientService hubRouterClientService;

    @Override
    public void handler(SensorsSnapshotAvro snapshotAvro, String hubId) {
        log.info("=== Обработка снапшота для хаба: {} ===", hubId);

        if (snapshotAvro.getSensorsState() == null || snapshotAvro.getSensorsState().isEmpty()) {
            log.warn("❌ Нет данных о сенсорах в снапшоте для хаба {}", hubId);
            return;
        }

        final Map<String, SensorStateAvro> sensorStateAvroMap = snapshotAvro.getSensorsState();
        final List<ScenarioProjection> scenarios;

        try {
            scenarios = scenarioRepository.findScenariosWithDetailsByHubId(hubId);
        } catch (Exception e) {
            log.error("Ошибка при загрузке сценариев для хаба {}: {}", hubId, e.getMessage(), e);
            return;
        }

        log.info("🔍 Найдено сценариев в БД для хаба {}: {}", hubId, scenarios.size());

        Map<Long, List<ScenarioProjection>> scenariosById = scenarios.stream()
                .collect(Collectors.groupingBy(ScenarioProjection::getScenarioId));


        for (Map.Entry<Long, List<ScenarioProjection>> entry : scenariosById.entrySet()) {
            Long scenarioId = entry.getKey();
            List<ScenarioProjection> scenarioDetails = entry.getValue();
            String scenarioName = scenarioDetails.get(0).getScenarioName();

            log.info("=== Проверяем сценарий '{}' для хаба {} ===", scenarioName, hubId);

            try {
                boolean allConditionsMet = areAllConditionsMet(scenarioDetails, sensorStateAvroMap);
                log.info("Условия сценария '{}' выполнены: {}", scenarioName, allConditionsMet);


                if (allConditionsMet) {
                    log.info("✅ АКТИВАЦИЯ СЦЕНАРИЯ '{}'", scenarioName);
                    activateScenario(scenarioId, scenarioName, hubId, scenarioDetails);
                }
            } catch (Exception e) {
                log.error("Ошибка при проверке сценария '{}' для хаба {}: {}",
                        scenarioName, hubId, e.getMessage(), e);
            }
        }
    }

    private boolean areAllConditionsMet(List<ScenarioProjection> scenarioDetails,
                                        Map<String, SensorStateAvro> sensorStates) {
        List<ScenarioProjection> conditions = scenarioDetails.stream()
                .filter(detail -> detail.getConditionType() != null)
                .collect(Collectors.toList());

        if (conditions.isEmpty()) {
            log.warn("❌ Нет условий для проверки");
            return false;
        }

        for (ScenarioProjection condition : conditions) {
            SensorStateAvro sensorState = sensorStates.get(condition.getSensorId());
            if (sensorState == null) {
                log.warn("❌ Сенсор {} не найден в снапшоте", condition.getSensorId());
                return false;
            }

            Object sensorData = sensorState.getData();
            if (sensorData == null) {
                log.warn("❌ Данные сенсора {} равны null", condition.getSensorId());
                return false;
            }

            boolean conditionMet = isConditionMet(condition, sensorState);
            log.info("Проверка условия для сенсора {}: {}", condition.getSensorId(), conditionMet);

            if (!conditionMet) {
                return false;
            }
        }
        return true;
    }

    private boolean isConditionMet(ScenarioProjection condition, SensorStateAvro sensorState) {
        Object sensorData = sensorState.getData();
        log.info("Данные сенсора {}: {}, тип: {}",
                condition.getSensorId(),
                sensorData,
                sensorData != null ? sensorData.getClass().getName() : "null");


        switch (condition.getConditionType()) {
            case "TEMPERATURE":
                return checkTemperatureCondition(condition, sensorData);
            case "MOTION":
                return checkMotionCondition(condition, sensorData);
            case "SWITCH":
                return checkSwitchCondition(condition, sensorData);
            case "LUMINOSITY":
                return checkLuminosityCondition(condition, sensorData);
            case "HUMIDITY":
            case "CO2LEVEL":
                return checkNumericCondition(condition, sensorData,
                        condition.getConditionType().equals("HUMIDITY") ? "humidity" : "co2Level");
            default:
                log.warn("❌ Неизвестный тип условия: {}", condition.getConditionType());
                return false;
        }
    }

    private boolean checkTemperatureCondition(ScenarioProjection condition, Object sensorData) {
        if (!(sensorData instanceof GenericRecord)) {
            log.warn("❌ Данные сенсора температуры не являются GenericRecord");
            return false;
        }

        GenericRecord record = (GenericRecord) sensorData;
        Object temperatureObj = null;

        for (String fieldName : TEMPERATURE_FIELDS) {
            if (record.hasField(fieldName)) {
                temperatureObj = record.get(fieldName);
                break;
            }
        }

        if (temperatureObj == null) {
            List<String> availableFields = record.getSchema().getFields().stream()
                    .map(org.apache.avro.Schema.Field::name)
                    .collect(Collectors.toList());
            log.warn("❌ Температура не найдена. Доступные поля: {}", availableFields);
            return false;
        }

        if (!(temperatureObj instanceof Number)) {
            log.warn("❌ Значение температуры не является числом: {}", temperatureObj);
            return false;
        }

        double temperature = ((Number) temperatureObj).doubleValue();
        log.info("✅ Температура: {:.1f}°C", temperature);
        return checkNumericCondition(condition, temperature);
    }

    private boolean checkMotionCondition(ScenarioProjection condition, Object sensorData) {
        if (!(sensorData instanceof GenericRecord)) {
            log.warn("❌ Данные сенсора движения не являются GenericRecord");
            return false;
        }

        GenericRecord record = (GenericRecord) sensorData;
        Object motionObj = record.get(FIELD_MOTION);

        if (!(motionObj instanceof Boolean)) {
            log.warn("❌ Значение движения не является boolean: {}", motionObj);
            return false;
        }

        boolean motion = (Boolean) motionObj;
        log.info("Движение: {}, условие: {} {}",
                motion, condition.getConditionOperation(), condition.getConditionValue());
        return checkBooleanCondition(condition, motion);
    }

    private boolean checkSwitchCondition(ScenarioProjection condition, Object sensorData) {
        if (!(sensorData instanceof GenericRecord)) {
            log.warn("❌ Данные переключателя не являются GenericRecord");
            return false;
        }

        GenericRecord record = (GenericRecord) sensorData;
        Object switchObj = record.get(FIELD_SWITCH);

        if (!(switchObj instanceof Boolean)) {
            log.warn("❌ Значение переключателя не является boolean: {}", switchObj);
            return false;
        }

        boolean switchState = (Boolean) switchObj;
        log.info("Переключатель: {}, условие: {} {}",
                switchState, condition.getConditionOperation(), condition.getConditionValue());
        return checkBooleanCondition(condition, switchState);
    }

    private boolean checkLuminosityCondition(ScenarioProjection condition, Object sensorData) {
        if (!(sensorData instanceof GenericRecord)) {
            log.warn("❌ Данные освещённости не являются GenericRecord");
            return false;
        }

        GenericRecord record = (GenericRecord) sensorData;
        Object lumObj = record.get(FIELD_LUMINOSITY);

        if (!(lumObj instanceof Integer)) {
            log.warn("❌ Значение освещённости не является целым числом: {}", lumObj);
            return false;
        }

        int luminosity = (Integer) lumObj;
        log.info("Освещённость: {}, условие: {} {}",
                luminosity, condition.getConditionOperation(), condition.getConditionValue());
        return checkNumericCondition(condition, luminosity);
    }

    private boolean checkNumericCondition(ScenarioProjection condition, Object sensorData, String fieldName) {
        if (!(sensorData instanceof GenericRecord)) {
            log.warn("❌ Данные сенсора не являются GenericRecord");
            return false;
        }

        GenericRecord record = (GenericRecord) sensorData;
        Object valueObj = record.get(fieldName);

        if (!(valueObj instanceof Integer)) {
            log.warn("❌ Значение поля {} не является целым числом: {}", fieldName, valueObj);
            return false;
        }

        int value = (Integer) valueObj;
        log.info("{}: {}, условие: {} {}",
                fieldName, value, condition.getConditionOperation(), condition.getConditionValue());
        return checkNumericCondition(condition, value);
    }

    private boolean checkNumericCondition(ScenarioProjection condition, double sensorValue) {
        Double conditionValue = Optional.ofNullable(condition.getConditionValue())
                .map(Integer::doubleValue)
                .orElse(null);

        if (conditionValue == null) {
            log.warn("❌ Отсутствует значение условия для числовой проверки");
            return false;
        }

        String operation = condition.getConditionOperation();
        log.info("Операция: {}, сенсор: {:.1f}, условие: {:.1f}",
                operation, sensorValue, conditionValue);

        boolean result;
        switch (operation.toUpperCase()) {
            case "GREATER_THAN":
            case "GT":
                result = sensorValue > conditionValue;
                break;
            case "LOWER_THAN":
            case "LT":
                result = sensorValue < conditionValue;
                break;
            case "EQUALS":
            case "EQ":
                result = Double.compare(sensorValue, conditionValue) == 0;
                break;
            case "GREATER_THAN_OR_EQUALS":
            case "GTE":
                result = sensorValue >= conditionValue;
                break;
            case "LOWER_THAN_OR_EQUALS":
            case "LTE":
                result = sensorValue <= conditionValue;
                break;
            default:
                log.warn("❌ Неизвестная операция: {}", operation);
                return false;
        }

        log.info("{:.1f} {} {:.1f} = {}", sensorValue, operation, conditionValue, result);
        return result;
    }

    private boolean checkBooleanCondition(ScenarioProjection condition, boolean sensorValue) {
        if (!"EQUALS".equals(condition.getConditionOperation())) {
            log.warn("❌ Неподдерживаемая операция для boolean: {}", condition.getConditionOperation());
            return false;
        }

        Integer conditionValue = condition.getConditionValue();
        boolean conditionBool = conditionValue != null && conditionValue != 0;
        boolean result = sensorValue == conditionBool;
        log.info("{} == {} = {}", sensorValue, conditionBool, result);
        return result;
    }

    private void activateScenario(Long scenarioId, String scenarioName, String hubId,
                                  List<ScenarioProjection> scenarioDetails) {
        List<ScenarioProjection> actions = scenarioDetails.stream()
                .filter(detail -> detail.getActionType() != null && detail.getActionSensorId() != null)
                .collect(Collectors.toList());

        log.info("🎯 Выполняем {} действий для сценария '{}'", actions.size(), scenarioName);


        for (ScenarioProjection actionDetail : actions) {
            try {
                log.info("🚀 Действие: сенсор={}, тип={}, значение={}",
                        actionDetail.getActionSensorId(),
                        actionDetail.getActionType(),
                        actionDetail.getActionValue());

                DeviceActionProto action = convertToDeviceActionProto(actionDetail);
                hubRouterClientService.sendDeviceAction(hubId, scenarioName, action);
            } catch (Exception e) {
                log.error("Ошибка при отправке действия для сенсора {} в сценарии '{}': {}",
                        actionDetail.getActionSensorId(), scenarioName, e.getMessage(), e);
            }
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
        try {
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
        } catch (IllegalArgumentException e) {
            log.error("Неизвестный тип действия: {}", actionType);
            throw e;
        }
    }
}
