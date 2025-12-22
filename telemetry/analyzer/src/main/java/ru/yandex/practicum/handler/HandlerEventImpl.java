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

import java.util.Arrays;
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
    private static final String FIELD_HUMIDITY = "humidity";
    private static final String FIELD_CO2LEVEL = "co2Level";

    private final ScenarioRepository scenarioRepository;
    private final HubRouterClientService hubRouterClientService;

    @Override
    public void handler(SensorsSnapshotAvro snapshotAvro, String hubId) {
        log.info("=== НАЧАЛО ОБРАБОТКИ СНАПШОТА ДЛЯ ХАБА: {} ===", hubId);

        if (snapshotAvro.getSensorsState() == null || snapshotAvro.getSensorsState().isEmpty()) {
            log.warn("❌ Нет данных о сенсорах в снапшоте для хаба {}", hubId);
            return;
        }

        // Детальное логирование входящих данных
        log.info("📊 Количество сенсоров в снапшоте: {}", snapshotAvro.getSensorsState().size());
        snapshotAvro.getSensorsState().forEach((sensorId, state) -> {
            log.info("🔌 Сенсор {}: данные = {}", sensorId, state.getData());
            if (state.getData() instanceof GenericRecord) {
                GenericRecord record = (GenericRecord) state.getData();
                List<String> fields = record.getSchema().getFields().stream()
                        .map(org.apache.avro.Schema.Field::name)
                        .collect(Collectors.toList());
                log.info("   📋 Поля сенсора {}: {}", sensorId, fields);
            }
        });

        final Map<String, SensorStateAvro> sensorStateAvroMap = snapshotAvro.getSensorsState();
        final List<ScenarioProjection> scenarios;

        try {
            scenarios = scenarioRepository.findScenariosWithDetailsByHubId(hubId);
            log.info("🔍 Найдено сценариев в БД для хаба {}: {}", hubId, scenarios.size());

            if (scenarios.isEmpty()) {
                log.warn("⚠️ Нет сценариев для хаба {} в базе данных", hubId);
                return;
            }

            // Логируем найденные сценарии
            scenarios.forEach(scenario ->
                    log.info("📝 Сценарий: '{}' (ID: {}), тип условия: {}, сенсор: {}",
                            scenario.getScenarioName(), scenario.getScenarioId(),
                            scenario.getConditionType(), scenario.getSensorId())
            );

        } catch (Exception e) {
            log.error("❌ Ошибка при загрузке сценариев для хаба {}: {}", hubId, e.getMessage(), e);
            return;
        }

        Map<Long, List<ScenarioProjection>> scenariosById = scenarios.stream()
                .collect(Collectors.groupingBy(ScenarioProjection::getScenarioId));

        log.info("🎯 Начинаем проверку {} сценариев для хаба {}", scenariosById.size(), hubId);

        for (Map.Entry<Long, List<ScenarioProjection>> entry : scenariosById.entrySet()) {
            Long scenarioId = entry.getKey();
            List<ScenarioProjection> scenarioDetails = entry.getValue();
            String scenarioName = scenarioDetails.get(0).getScenarioName();

            log.info("=== ПРОВЕРКА СЦЕНАРИЯ '{}' (ID: {}) ДЛЯ ХАБА {} ===",
                    scenarioName, scenarioId, hubId);

            try {
                boolean allConditionsMet = areAllConditionsMet(scenarioDetails, sensorStateAvroMap);
                log.info("📊 Условия сценария '{}' выполнены: {}", scenarioName, allConditionsMet);

                if (allConditionsMet) {
                    log.info("✅ АКТИВАЦИЯ СЦЕНАРИЯ '{}'", scenarioName);
                    activateScenario(scenarioId, scenarioName, hubId, scenarioDetails);
                } else {
                    log.info("❌ Сценарий '{}' не активирован - условия не выполнены", scenarioName);
                }
            } catch (Exception e) {
                log.error("❌ Ошибка при проверке сценария '{}' для хаба {}: {}",
                        scenarioName, hubId, e.getMessage(), e);
            }
        }

        log.info("=== ЗАВЕРШЕНИЕ ОБРАБОТКИ СНАПШОТА ДЛЯ ХАБА: {} ===", hubId);
    }

    private boolean areAllConditionsMet(List<ScenarioProjection> scenarioDetails,
                                        Map<String, SensorStateAvro> sensorStates) {
        List<ScenarioProjection> conditions = scenarioDetails.stream()
                .filter(detail -> detail.getConditionType() != null && detail.getSensorId() != null)
                .collect(Collectors.toList());

        if (conditions.isEmpty()) {
            log.warn("⚠️ Нет условий для проверки в сценарии");
            return false;
        }

        log.info("🔍 Проверяем {} условий сценария", conditions.size());

        for (ScenarioProjection condition : conditions) {
            String sensorId = condition.getSensorId();
            SensorStateAvro sensorState = sensorStates.get(sensorId);

            if (sensorState == null) {
                log.warn("❌ Сенсор {} не найден в снапшоте. Доступные сенсоры: {}",
                        sensorId, sensorStates.keySet());
                return false;
            }

            Object sensorData = sensorState.getData();
            if (sensorData == null) {
                log.warn("❌ Данные сенсора {} равны null", sensorId);
                return false;
            }

            // Детальный дебаг для проблемных сенсоров
            if ("TEMPERATURE".equals(condition.getConditionType())) {
                debugSensorData(sensorId, sensorData, "ТЕМПЕРАТУРА");
            }

            boolean conditionMet = isConditionMet(condition, sensorState);
            log.info("📋 Проверка условия для сенсора {} (тип: {}): {}",
                    sensorId, condition.getConditionType(), conditionMet);

            if (!conditionMet) {
                log.info("❌ Условие не выполнено для сенсора {}", sensorId);
                return false;
            }
        }

        log.info("✅ Все условия сценария выполнены");
        return true;
    }

    private boolean isConditionMet(ScenarioProjection condition, SensorStateAvro sensorState) {
        Object sensorData = sensorState.getData();
        String sensorId = condition.getSensorId();

        log.info("🔎 Анализ сенсора {}: данные = {}, тип данных = {}",
                sensorId, sensorData,
                sensorData != null ? sensorData.getClass().getSimpleName() : "null");

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
                return checkNumericCondition(condition, sensorData, FIELD_HUMIDITY);
            case "CO2LEVEL":
                return checkNumericCondition(condition, sensorData, FIELD_CO2LEVEL);
            default:
                log.warn("❌ Неизвестный тип условия: {}", condition.getConditionType());
                return false;
        }
    }

    private boolean checkTemperatureCondition(ScenarioProjection condition, Object sensorData) {
        log.info("🔍 АНАЛИЗ ТЕМПЕРАТУРНОГО СЕНСОРА {}", condition.getSensorId());

        if (!(sensorData instanceof GenericRecord)) {
            log.warn("❌ Данные сенсора температуры не являются GenericRecord. Тип: {}",
                    sensorData != null ? sensorData.getClass().getName() : "null");
            return false;
        }

        GenericRecord record = (GenericRecord) sensorData;

        // Логируем все доступные поля
        List<String> availableFields = record.getSchema().getFields().stream()
                .map(org.apache.avro.Schema.Field::name)
                .collect(Collectors.toList());
        log.info("📋 Доступные поля в сенсоре {}: {}", condition.getSensorId(), availableFields);

        // Логируем значения всех полей
        for (String field : availableFields) {
            Object value = record.get(field);
            log.info("   {} = {} (тип: {})",
                    field, value, value != null ? value.getClass().getSimpleName() : "null");
        }

        Object temperatureObj = null;
        String foundField = null;

        // Сначала ищем в стандартных полях температуры
        for (String fieldName : TEMPERATURE_FIELDS) {
            if (record.hasField(fieldName)) {
                temperatureObj = record.get(fieldName);
                foundField = fieldName;
                log.info("✅ Найдена температура в стандартном поле '{}': {}", fieldName, temperatureObj);
                break;
            }
        }

        // Если не нашли в стандартных полях, ищем любое числовое поле
        if (temperatureObj == null) {
            log.info("🔎 Температура не найдена в стандартных полях, ищем любое числовое поле...");
            for (String field : availableFields) {
                Object value = record.get(field);
                if (value instanceof Number) {
                    temperatureObj = value;
                    foundField = field;
                    log.info("🎯 Используем поле '{}' как температуру: {}", field, value);
                    break;
                }
            }
        }

        if (temperatureObj == null) {
            log.warn("❌ Температура не найдена. Стандартные поля: {}. Доступные поля: {}",
                    Arrays.toString(TEMPERATURE_FIELDS), availableFields);
            return false;
        }

        log.info("✅ Найдена температура в поле '{}': {}", foundField, temperatureObj);

        if (!(temperatureObj instanceof Number)) {
            log.warn("❌ Значение температуры не является числом: {} (тип: {})",
                    temperatureObj, temperatureObj.getClass().getName());
            return false;
        }

        double temperature = ((Number) temperatureObj).doubleValue();
        log.info("🌡️ Извлечённая температура: {:.1f}°C", temperature);

        boolean result = checkNumericCondition(condition, temperature);
        log.info("📊 Результат проверки температуры: {}", result);
        return result;
    }

    private boolean checkMotionCondition(ScenarioProjection condition, Object sensorData) {
        log.info("🔍 Анализ сенсора движения {}", condition.getSensorId());

        if (!(sensorData instanceof GenericRecord)) {
            log.warn("❌ Данные сенсора движения не являются GenericRecord");
            return false;
        }

        GenericRecord record = (GenericRecord) sensorData;
        Object motionObj = record.get(FIELD_MOTION);

        if (motionObj == null) {
            log.warn("❌ Поле '{}' не найдено в данных сенсора движения", FIELD_MOTION);
            return false;
        }

        if (!(motionObj instanceof Boolean)) {
            log.warn("❌ Значение движения не является boolean: {} (тип: {})",
                    motionObj, motionObj.getClass().getSimpleName());
            return false;
        }

        boolean motion = (Boolean) motionObj;
        log.info("🏃 Движение: {}, условие: {} {}",
                motion, condition.getConditionOperation(), condition.getConditionValue());

        boolean result = checkBooleanCondition(condition, motion);
        log.info("📊 Результат проверки движения: {}", result);
        return result;
    }

    private boolean checkSwitchCondition(ScenarioProjection condition, Object sensorData) {
        log.info("🔍 Анализ переключателя {}", condition.getSensorId());

        if (!(sensorData instanceof GenericRecord)) {
            log.warn("❌ Данные переключателя не являются GenericRecord");
            return false;
        }

        GenericRecord record = (GenericRecord) sensorData;
        Object switchObj = record.get(FIELD_SWITCH);

        if (switchObj == null) {
            log.warn("❌ Поле '{}' не найдено в данных переключателя", FIELD_SWITCH);
            return false;
        }

        if (!(switchObj instanceof Boolean)) {
            log.warn("❌ Значение переключателя не является boolean: {} (тип: {})",
                    switchObj, switchObj.getClass().getSimpleName());
            return false;
        }

        boolean switchState = (Boolean) switchObj;
        log.info("🔌 Переключатель: {}, условие: {} {}",
                switchState, condition.getConditionOperation(), condition.getConditionValue());

        boolean result = checkBooleanCondition(condition, switchState);
        log.info("📊 Результат проверки переключателя: {}", result);
        return result;
    }

    private boolean checkLuminosityCondition(ScenarioProjection condition, Object sensorData) {
        log.info("🔍 Анализ сенсора освещённости {}", condition.getSensorId());

        if (!(sensorData instanceof GenericRecord)) {
            log.warn("❌ Данные освещённости не являются GenericRecord");
            return false;
        }

        GenericRecord record = (GenericRecord) sensorData;
        Object lumObj = record.get(FIELD_LUMINOSITY);

        if (lumObj == null) {
            log.warn("❌ Поле '{}' не найдено в данных освещённости", FIELD_LUMINOSITY);
            return false;
        }

        if (!(lumObj instanceof Integer)) {
            log.warn("❌ Значение освещённости не является целым числом: {} (тип: {})",
                    lumObj, lumObj.getClass().getSimpleName());
            return false;
        }

        int luminosity = (Integer) lumObj;
        log.info("💡 Освещённость: {}, условие: {} {}",
                luminosity, condition.getConditionOperation(), condition.getConditionValue());

        boolean result = checkNumericCondition(condition, luminosity);
        log.info("📊 Результат проверки освещённости: {}", result);
        return result;
    }

    private boolean checkNumericCondition(ScenarioProjection condition, Object sensorData, String fieldName) {
        log.info("🔍 Анализ сенсора {} (поле: {})", condition.getSensorId(), fieldName);

        if (!(sensorData instanceof GenericRecord)) {
            log.warn("❌ Данные сенсора не являются GenericRecord");
            return false;
        }

        GenericRecord record = (GenericRecord) sensorData;
        Object valueObj = record.get(fieldName);

        if (valueObj == null) {
            log.warn("❌ Поле '{}' не найдено в данных сенсора", fieldName);
            return false;
        }

        if (!(valueObj instanceof Integer)) {
            log.warn("❌ Значение поля {} не является целым числом: {} (тип: {})",
                    fieldName, valueObj, valueObj.getClass().getSimpleName());
            return false;
        }

        int value = (Integer) valueObj;
        log.info("📈 {}: {}, условие: {} {}",
                fieldName, value, condition.getConditionOperation(), condition.getConditionValue());

        boolean result = checkNumericCondition(condition, value);
        log.info("📊 Результат проверки {}: {}", fieldName, result);
        return result;
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
        log.info("⚙️ Операция: {}, сенсор: {:.1f}, условие: {:.1f}",
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

        log.info("🔢 {:.1f} {} {:.1f} = {}", sensorValue, operation, conditionValue, result);
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

        log.info("🔘 {} == {} = {}", sensorValue, conditionBool, result);
        return result;
    }

    private void activateScenario(Long scenarioId, String scenarioName, String hubId,
                                  List<ScenarioProjection> scenarioDetails) {
        List<ScenarioProjection> actions = scenarioDetails.stream()
                .filter(detail -> detail.getActionType() != null && detail.getActionSensorId() != null)
                .collect(Collectors.toList());

        if (actions.isEmpty()) {
            log.warn("⚠️ Нет действий для выполнения в сценарии '{}'", scenarioName);
            return;
        }

        log.info("🎯 Выполняем {} действий для сценария '{}'", actions.size(), scenarioName);

        for (ScenarioProjection actionDetail : actions) {
            try {
                log.info("🚀 Действие: сенсор={}, тип={}, значение={}",
                        actionDetail.getActionSensorId(),
                        actionDetail.getActionType(),
                        actionDetail.getActionValue());

                DeviceActionProto action = convertToDeviceActionProto(actionDetail);
                hubRouterClientService.sendDeviceAction(hubId, scenarioName, action);
                log.info("✅ Действие успешно отправлено для сенсора {}", actionDetail.getActionSensorId());
            } catch (Exception e) {
                log.error("❌ Ошибка при отправке действия для сенсора {} в сценарии '{}': {}",
                        actionDetail.getActionSensorId(), scenarioName, e.getMessage(), e);
            }
        }

        log.info("✅ Все действия сценария '{}' обработаны", scenarioName);
    }

    private DeviceActionProto convertToDeviceActionProto(ScenarioProjection actionDetail) {
        DeviceActionProto.Builder builder = DeviceActionProto.newBuilder()
                .setSensorId(actionDetail.getActionSensorId())
                .setType(convertActionType(actionDetail.getActionType()));

        if (actionDetail.getActionValue() != null) {
            builder.setValue(actionDetail.getActionValue());
        }

        log.info("🔄 Преобразовано в DeviceActionProto: sensorId={}, type={}, value={}",
                actionDetail.getActionSensorId(), actionDetail.getActionType(), actionDetail.getActionValue());

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
            log.error("❌ Неизвестный тип действия: {}", actionType);
            throw e;
        }
    }

    // Вспомогательный метод для детального дебага данных сенсора
    private void debugSensorData(String sensorId, Object sensorData, String sensorType) {
        log.info("=== ДЕБАГ {} СЕНСОРА {} ===", sensorType, sensorId);
        if (sensorData instanceof GenericRecord) {
            GenericRecord record = (GenericRecord) sensorData;
            org.apache.avro.Schema schema = record.getSchema();
            log.info("📐 Схема Avro: {}", schema.getFullName());
            for (org.apache.avro.Schema.Field field : schema.getFields()) {
                Object value = record.get(field.name());
                log.info("   📌 Поле '{}': {} (тип: {})",
                        field.name(), value, value != null ? value.getClass().getSimpleName() : "null");
            }
        } else {
            log.info("📦 Тип данных: {}", sensorData.getClass().getName());
            log.info("📦 Данные: {}", sensorData);
        }
        log.info("=== КОНЕЦ ДЕБАГА ===");
    }
}