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

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class HandlerEventImpl implements HandlerEvent {

    private static final String[] TEMPERATURE_FIELDS = {"temperatureC", "temperature", "temp", "value", "temperature_c"};
    private static final String FIELD_LUMINOSITY = "luminosity";
    private static final String FIELD_MOTION = "motion";
    private static final String FIELD_SWITCH = "stat";
    private static final String FIELD_HUMIDITY = "humidity";
    private static final String FIELD_CO2LEVEL = "co2Level";

    private final ScenarioRepository scenarioRepository;
    private final HubRouterClientService hubRouterClientService;

    @Override
    public void handler(SensorsSnapshotAvro snapshotAvro, String hubId) {
        System.out.println("\n=== GITHUB_DEBUG_HANDLER_START ===");
        System.out.println("📦 СНАПШОТ ПОЛУЧЕН:");
        System.out.println("  Hub ID: " + hubId);
        System.out.println("  Timestamp снапшота: " + snapshotAvro.getTimestamp());
        System.out.println("  Сенсоров: " + (snapshotAvro.getSensorsState() != null ? snapshotAvro.getSensorsState().size() : 0));

        log.info("=== НАЧАЛО ОБРАБОТКИ СНАПШОТА ДЛЯ ХАБА: {} ===", hubId);

        // 1. Проверяем входные данные
        if (snapshotAvro.getSensorsState() == null || snapshotAvro.getSensorsState().isEmpty()) {
            System.out.println("❌ НЕТ ДАННЫХ О СЕНСОРАХ");
            log.warn("❌ Нет данных о сенсорах в снапшоте для хаба {}", hubId);
            return;
        }

        // 2. Детальное логирование входящих данных
        System.out.println("\n📊 ДЕТАЛИ СНАПШОТА:");
        final Map<String, SensorStateAvro> sensorStateAvroMap = snapshotAvro.getSensorsState();
        sensorStateAvroMap.forEach((sensorId, state) -> {
            System.out.println("  🔌 Сенсор: " + sensorId);
            System.out.println("    Timestamp сенсора: " + state.getTimestamp());

            Object data = state.getData();
            if (data == null) {
                System.out.println("    Данные: NULL");
            } else if (data instanceof GenericRecord) {
                GenericRecord record = (GenericRecord) data;
                System.out.println("    Тип данных: GenericRecord (" + record.getSchema().getName() + ")");

                // Логируем все поля
                for (org.apache.avro.Schema.Field field : record.getSchema().getFields()) {
                    Object value = record.get(field.name());
                    String valueStr = (value != null) ? value.toString() : "null";
                    String typeStr = (value != null) ? value.getClass().getSimpleName() : "null";
                    System.out.println("      " + field.name() + ": " + valueStr + " (тип: " + typeStr + ")");
                }
            } else {
                System.out.println("    Данные: " + data + " (тип: " + data.getClass().getSimpleName() + ")");
            }
        });

        // 3. Загружаем сценарии из БД
        final List<ScenarioProjection> scenarios;
        try {
            System.out.println("\n🔍 ЗАГРУЗКА СЦЕНАРИЕВ ИЗ БД ДЛЯ ХАБА: " + hubId);
            scenarios = scenarioRepository.findScenariosWithDetailsByHubId(hubId);
            System.out.println("✅ Найдено сценариев: " + scenarios.size());
            log.info("🔍 Найдено сценариев в БД для хаба {}: {}", hubId, scenarios.size());

            if (scenarios.isEmpty()) {
                System.out.println("⚠️ НЕТ СЦЕНАРИЕВ ДЛЯ ХАБА " + hubId);
                log.warn("⚠️ Нет сценариев для хаба {} в базе данных", hubId);
                return;
            }

            // Логируем найденные сценарии
            System.out.println("\n📝 НАЙДЕННЫЕ СЦЕНАРИИ:");
            scenarios.forEach(scenario -> {
                System.out.println("  Сценарий: '" + scenario.getScenarioName() + "' (ID: " + scenario.getScenarioId() + ")");

                if (scenario.getConditionType() != null) {
                    System.out.println("    Условие: тип=" + scenario.getConditionType() +
                            ", операция=" + scenario.getConditionOperation() +
                            ", значение=" + scenario.getConditionValue() +
                            ", сенсор=" + scenario.getSensorId());
                }

                if (scenario.getActionType() != null) {
                    System.out.println("    Действие: тип=" + scenario.getActionType() +
                            ", значение=" + scenario.getActionValue() +
                            ", сенсор=" + scenario.getActionSensorId());
                }
            });

        } catch (Exception e) {
            System.out.println("❌ ОШИБКА ПРИ ЗАГРУЗКЕ СЦЕНАРИЕВ: " + e.getMessage());
            e.printStackTrace();
            log.error("❌ Ошибка при загрузке сценариев для хаба {}: {}", hubId, e.getMessage(), e);
            return;
        }

        // 4. Группируем сценарии по ID
        Map<Long, List<ScenarioProjection>> scenariosById = scenarios.stream()
                .collect(Collectors.groupingBy(ScenarioProjection::getScenarioId));

        System.out.println("\n🎯 НАЧИНАЕМ ПРОВЕРКУ " + scenariosById.size() + " СЦЕНАРИЕВ");
        log.info("🎯 Начинаем проверку {} сценариев для хаба {}", scenariosById.size(), hubId);

        // 5. Проверяем каждый сценарий
        for (Map.Entry<Long, List<ScenarioProjection>> entry : scenariosById.entrySet()) {
            Long scenarioId = entry.getKey();
            List<ScenarioProjection> scenarioDetails = entry.getValue();
            String scenarioName = scenarioDetails.get(0).getScenarioName();

            System.out.println("\n=== ПРОВЕРКА СЦЕНАРИЯ '" + scenarioName + "' (ID: " + scenarioId + ") ===");
            log.info("=== ПРОВЕРКА СЦЕНАРИЯ '{}' (ID: {}) ДЛЯ ХАБА {} ===",
                    scenarioName, scenarioId, hubId);

            try {
                boolean allConditionsMet = areAllConditionsMet(scenarioDetails, sensorStateAvroMap);
                System.out.println("📊 УСЛОВИЯ СЦЕНАРИЯ '" + scenarioName + "' ВЫПОЛНЕНЫ: " + allConditionsMet);
                log.info("📊 Условия сценария '{}' выполнены: {}", scenarioName, allConditionsMet);

                if (allConditionsMet) {
                    System.out.println("✅ АКТИВАЦИЯ СЦЕНАРИЯ '" + scenarioName + "'");
                    log.info("✅ АКТИВАЦИЯ СЦЕНАРИЯ '{}'", scenarioName);
                    activateScenario(scenarioId, scenarioName, hubId, scenarioDetails);
                } else {
                    System.out.println("❌ СЦЕНАРИЙ '" + scenarioName + "' НЕ АКТИВИРОВАН - УСЛОВИЯ НЕ ВЫПОЛНЕНЫ");
                    log.info("❌ Сценарий '{}' не активирован - условия не выполнены", scenarioName);
                }
            } catch (Exception e) {
                System.out.println("❌ ОШИБКА ПРИ ПРОВЕРКЕ СЦЕНАРИЯ '" + scenarioName + "': " + e.getMessage());
                e.printStackTrace();
                log.error("❌ Ошибка при проверке сценария '{}' для хаба {}: {}",
                        scenarioName, hubId, e.getMessage(), e);
            }
        }

        System.out.println("=== GITHUB_DEBUG_HANDLER_END ===\n");
        log.info("=== ЗАВЕРШЕНИЕ ОБРАБОТКИ СНАПШОТА ДЛЯ ХАБА: {} ===\n", hubId);
    }

    private boolean areAllConditionsMet(List<ScenarioProjection> scenarioDetails,
                                        Map<String, SensorStateAvro> sensorStates) {
        // Фильтруем только условия (actionType == null)
        List<ScenarioProjection> conditions = scenarioDetails.stream()
                .filter(detail -> detail.getConditionType() != null &&
                        detail.getConditionType() != null &&
                        detail.getSensorId() != null)
                .collect(Collectors.toList());

        System.out.println("  🔍 Проверяем " + conditions.size() + " условий сценария");

        if (conditions.isEmpty()) {
            System.out.println("  ⚠️ НЕТ УСЛОВИЙ ДЛЯ ПРОВЕРКИ");
            log.warn("⚠️ Нет условий для проверки в сценарии");
            return false;
        }

        for (ScenarioProjection condition : conditions) {
            String sensorId = condition.getSensorId();

            System.out.println("\n  📋 ПРОВЕРКА УСЛОВИЯ:");
            System.out.println("    Сенсор: " + sensorId);
            System.out.println("    Тип условия: " + condition.getConditionType());
            System.out.println("    Операция: " + condition.getConditionOperation());
            System.out.println("    Значение: " + condition.getConditionValue());

            SensorStateAvro sensorState = sensorStates.get(sensorId);

            if (sensorState == null) {
                System.out.println("    ❌ СЕНСОР НЕ НАЙДЕН В СНАПШОТЕ");
                System.out.println("    Доступные сенсоры: " + sensorStates.keySet());
                log.warn("❌ Сенсор {} не найден в снапшоте. Доступные сенсоры: {}",
                        sensorId, sensorStates.keySet());
                return false;
            }

            Object sensorData = sensorState.getData();
            if (sensorData == null) {
                System.out.println("    ❌ ДАННЫЕ СЕНСОРА РАВНЫ NULL");
                log.warn("❌ Данные сенсора {} равны null", sensorId);
                return false;
            }

            System.out.println("    📊 Данные сенсора: " + sensorData.getClass().getSimpleName());

            boolean conditionMet = isConditionMet(condition, sensorState);
            System.out.println("    📋 Результат проверки: " + conditionMet);

            if (!conditionMet) {
                System.out.println("    ❌ УСЛОВИЕ НЕ ВЫПОЛНЕНО ДЛЯ СЕНСОРА " + sensorId);
                log.info("❌ Условие не выполнено для сенсора {}", sensorId);
                return false;
            }
        }

        System.out.println("  ✅ ВСЕ УСЛОВИЯ СЦЕНАРИЯ ВЫПОЛНЕНЫ");
        log.info("✅ Все условия сценария выполнены");
        return true;
    }

    private boolean isConditionMet(ScenarioProjection condition, SensorStateAvro sensorState) {
        Object sensorData = sensorState.getData();
        String sensorId = condition.getSensorId();

        System.out.println("    🔎 Анализ сенсора " + sensorId + ":");
        System.out.println("      Тип данных: " + (sensorData != null ? sensorData.getClass().getSimpleName() : "null"));
        System.out.println("      Данные: " + sensorData);

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
                System.out.println("    ❌ НЕИЗВЕСТНЫЙ ТИП УСЛОВИЯ: " + condition.getConditionType());
                log.warn("❌ Неизвестный тип условия: {}", condition.getConditionType());
                return false;
        }
    }

    private boolean checkTemperatureCondition(ScenarioProjection condition, Object sensorData) {
        System.out.println("    🔍 АНАЛИЗ ТЕМПЕРАТУРНОГО СЕНСОРА " + condition.getSensorId());

        if (!(sensorData instanceof GenericRecord)) {
            System.out.println("    ❌ ДАННЫЕ СЕНСОРА ТЕМПЕРАТУРЫ НЕ ЯВЛЯЮТСЯ GenericRecord");
            System.out.println("    Тип: " + (sensorData != null ? sensorData.getClass().getName() : "null"));
            log.warn("❌ Данные сенсора температуры не являются GenericRecord. Тип: {}",
                    sensorData != null ? sensorData.getClass().getName() : "null");
            return false;
        }

        GenericRecord record = (GenericRecord) sensorData;

        // Логируем все доступные поля
        List<String> availableFields = new ArrayList<>();
        for (org.apache.avro.Schema.Field field : record.getSchema().getFields()) {
            availableFields.add(field.name());
        }
        System.out.println("    📋 Доступные поля в сенсоре: " + availableFields);

        Object temperatureObj = null;
        String foundField = null;

        // Ищем поле с температурой - ПРИОРИТЕТ ДЛЯ CLIMATE_SENSOR
        System.out.println("    🔎 Ищем поле с температурой...");

        // Сначала ищем temperature_c (для ClimateSensorProto)
        if (record.hasField("temperature_c")) {
            temperatureObj = record.get("temperature_c");
            foundField = "temperature_c";
            System.out.println("    ✅ Найдена температура в поле 'temperature_c': " + temperatureObj);
        }
        // Затем temperatureC (для TemperatureSensorProto)
        else if (record.hasField("temperatureC")) {
            temperatureObj = record.get("temperatureC");
            foundField = "temperatureC";
            System.out.println("    ✅ Найдена температура в поле 'temperatureC': " + temperatureObj);
        }
        // Затем temperature
        else if (record.hasField("temperature")) {
            temperatureObj = record.get("temperature");
            foundField = "temperature";
            System.out.println("    ✅ Найдена температура в поле 'temperature': " + temperatureObj);
        }
        // Затем temp
        else if (record.hasField("temp")) {
            temperatureObj = record.get("temp");
            foundField = "temp";
            System.out.println("    ✅ Найдена температура в поле 'temp': " + temperatureObj);
        }
        // Затем value
        else if (record.hasField("value")) {
            temperatureObj = record.get("value");
            foundField = "value";
            System.out.println("    ✅ Найдена температура в поле 'value': " + temperatureObj);
        }

        // Если не нашли в стандартных полях, ищем любое числовое поле
        if (temperatureObj == null) {
            System.out.println("    🔎 Температура не найдена в стандартных полях, ищем любое числовое поле...");
            for (String field : availableFields) {
                Object value = record.get(field);
                if (value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof Double) {
                    temperatureObj = value;
                    foundField = field;
                    System.out.println("    🎯 Используем поле '" + field + "' как температуру: " + value);
                    log.info("🎯 Используем поле '{}' как температуру: {}", field, value);
                    break;
                }
            }
        }

        if (temperatureObj == null) {
            System.out.println("    ❌ ТЕМПЕРАТУРА НЕ НАЙДЕНА");
            System.out.println("    Доступные поля: " + availableFields);
            log.warn("❌ Температура не найдена в полях: {}", availableFields);
            return false;
        }

        System.out.println("    ✅ Найдена температура в поле '" + foundField + "': " + temperatureObj);

        if (!(temperatureObj instanceof Number)) {
            System.out.println("    ❌ ЗНАЧЕНИЕ ТЕМПЕРАТУРЫ НЕ ЯВЛЯЕТСЯ ЧИСЛОМ");
            System.out.println("    Тип: " + temperatureObj.getClass().getName());
            log.warn("❌ Значение температуры не является числом: {} (тип: {})",
                    temperatureObj, temperatureObj.getClass().getName());
            return false;
        }

        double temperature = ((Number) temperatureObj).doubleValue();
        System.out.println("    🌡️ Извлечённая температура: " + temperature + "°C");
        log.info("🌡️ Извлечённая температура: {:.1f}°C", temperature);

        boolean result = checkNumericCondition(condition, temperature);
        System.out.println("    📊 Результат проверки температуры: " + result);
        log.info("📊 Результат проверки температуры: {}", result);
        return result;
    }

    private boolean checkMotionCondition(ScenarioProjection condition, Object sensorData) {
        System.out.println("    🔍 Анализ сенсора движения " + condition.getSensorId());

        if (!(sensorData instanceof GenericRecord)) {
            System.out.println("    ❌ Данные сенсора движения не являются GenericRecord");
            return false;
        }

        GenericRecord record = (GenericRecord) sensorData;
        Object motionObj = record.get(FIELD_MOTION);

        if (motionObj == null) {
            System.out.println("    ❌ Поле '" + FIELD_MOTION + "' не найдено в данных сенсора движения");
            return false;
        }

        if (!(motionObj instanceof Boolean)) {
            System.out.println("    ❌ Значение движения не является boolean: " + motionObj);
            return false;
        }

        boolean motion = (Boolean) motionObj;
        System.out.println("    🏃 Движение: " + motion);
        System.out.println("    Условие: " + condition.getConditionOperation() + " " + condition.getConditionValue());

        boolean result = checkBooleanCondition(condition, motion);
        System.out.println("    📊 Результат проверки движения: " + result);
        return result;
    }

    private boolean checkSwitchCondition(ScenarioProjection condition, Object sensorData) {
        System.out.println("    🔍 Анализ переключателя " + condition.getSensorId());

        if (!(sensorData instanceof GenericRecord)) {
            System.out.println("    ❌ Данные переключателя не являются GenericRecord");
            return false;
        }

        GenericRecord record = (GenericRecord) sensorData;
        Object switchObj = record.get(FIELD_SWITCH);

        if (switchObj == null) {
            System.out.println("    ❌ Поле '" + FIELD_SWITCH + "' не найдено в данных переключателя");
            return false;
        }

        if (!(switchObj instanceof Boolean)) {
            System.out.println("    ❌ Значение переключателя не является boolean: " + switchObj);
            return false;
        }

        boolean switchState = (Boolean) switchObj;
        System.out.println("    🔌 Переключатель: " + switchState);
        System.out.println("    Условие: " + condition.getConditionOperation() + " " + condition.getConditionValue());

        boolean result = checkBooleanCondition(condition, switchState);
        System.out.println("    📊 Результат проверки переключателя: " + result);
        return result;
    }

    private boolean checkLuminosityCondition(ScenarioProjection condition, Object sensorData) {
        System.out.println("    🔍 Анализ сенсора освещённости " + condition.getSensorId());

        if (!(sensorData instanceof GenericRecord)) {
            System.out.println("    ❌ Данные освещённости не являются GenericRecord");
            return false;
        }

        GenericRecord record = (GenericRecord) sensorData;
        Object lumObj = record.get(FIELD_LUMINOSITY);

        if (lumObj == null) {
            System.out.println("    ❌ Поле '" + FIELD_LUMINOSITY + "' не найдено в данных освещённости");
            return false;
        }

        if (!(lumObj instanceof Integer)) {
            System.out.println("    ❌ Значение освещённости не является целым числом: " + lumObj);
            return false;
        }

        int luminosity = (Integer) lumObj;
        System.out.println("    💡 Освещённость: " + luminosity);
        System.out.println("    Условие: " + condition.getConditionOperation() + " " + condition.getConditionValue());

        boolean result = checkNumericCondition(condition, luminosity);
        System.out.println("    📊 Результат проверки освещённости: " + result);
        return result;
    }

    private boolean checkNumericCondition(ScenarioProjection condition, Object sensorData, String fieldName) {
        System.out.println("    🔍 Анализ сенсора " + condition.getSensorId() + " (поле: " + fieldName + ")");

        if (!(sensorData instanceof GenericRecord)) {
            System.out.println("    ❌ Данные сенсора не являются GenericRecord");
            return false;
        }

        GenericRecord record = (GenericRecord) sensorData;
        Object valueObj = record.get(fieldName);

        if (valueObj == null) {
            System.out.println("    ❌ Поле '" + fieldName + "' не найдено в данных сенсора");
            return false;
        }

        if (!(valueObj instanceof Integer)) {
            System.out.println("    ❌ Значение поля " + fieldName + " не является целым числом: " + valueObj);
            return false;
        }

        int value = (Integer) valueObj;
        System.out.println("    📈 " + fieldName + ": " + value);
        System.out.println("    Условие: " + condition.getConditionOperation() + " " + condition.getConditionValue());

        boolean result = checkNumericCondition(condition, value);
        System.out.println("    📊 Результат проверки " + fieldName + ": " + result);
        return result;
    }

    private boolean checkNumericCondition(ScenarioProjection condition, double sensorValue) {
        Double conditionValue = Optional.ofNullable(condition.getConditionValue())
                .map(Integer::doubleValue)
                .orElse(null);

        if (conditionValue == null) {
            System.out.println("    ❌ ОТСУТСТВУЕТ ЗНАЧЕНИЕ УСЛОВИЯ ДЛЯ ЧИСЛОВОЙ ПРОВЕРКИ");
            log.warn("❌ Отсутствует значение условия для числовой проверки");
            return false;
        }

        String operation = condition.getConditionOperation();
        System.out.println("    ⚙️ Операция: " + operation);
        System.out.println("    Сенсор: " + sensorValue);
        System.out.println("    Условие: " + conditionValue);

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
                System.out.println("    ❌ НЕИЗВЕСТНАЯ ОПЕРАЦИЯ: " + operation);
                log.warn("❌ Неизвестная операция: {}", operation);
                return false;
        }

        System.out.println("    🔢 " + sensorValue + " " + operation + " " + conditionValue + " = " + result);
        return result;
    }

    private boolean checkBooleanCondition(ScenarioProjection condition, boolean sensorValue) {
        if (!"EQUALS".equals(condition.getConditionOperation())) {
            System.out.println("    ❌ Неподдерживаемая операция для boolean: " + condition.getConditionOperation());
            log.warn("❌ Неподдерживаемая операция для boolean: {}", condition.getConditionOperation());
            return false;
        }

        Integer conditionValue = condition.getConditionValue();
        boolean conditionBool = conditionValue != null && conditionValue != 0;
        boolean result = sensorValue == conditionBool;

        System.out.println("    🔘 " + sensorValue + " == " + conditionBool + " = " + result);
        return result;
    }

    private void activateScenario(Long scenarioId, String scenarioName, String hubId,
                                  List<ScenarioProjection> scenarioDetails) {
        // Фильтруем только действия (conditionType == null)
        List<ScenarioProjection> actions = scenarioDetails.stream()
                .filter(detail -> detail.getActionType() != null && detail.getActionSensorId() != null)
                .collect(Collectors.toList());

        System.out.println("  🎯 Найдено действий: " + actions.size());

        if (actions.isEmpty()) {
            System.out.println("  ⚠️ НЕТ ДЕЙСТВИЙ ДЛЯ ВЫПОЛНЕНИЯ");
            log.warn("⚠️ Нет действий для выполнения в сценарии '{}'", scenarioName);
            return;
        }

        System.out.println("  🎯 Выполняем " + actions.size() + " действий для сценария '" + scenarioName + "'");
        log.info("🎯 Выполняем {} действий для сценария '{}'", actions.size(), scenarioName);

        for (ScenarioProjection actionDetail : actions) {
            try {
                System.out.println("\n  🚀 ДЕЙСТВИЕ:");
                System.out.println("    Сенсор: " + actionDetail.getActionSensorId());
                System.out.println("    Тип: " + actionDetail.getActionType());
                System.out.println("    Значение: " + actionDetail.getActionValue());

                DeviceActionProto action = convertToDeviceActionProto(actionDetail);
                hubRouterClientService.sendDeviceAction(hubId, scenarioName, action);
                System.out.println("    ✅ Действие успешно отправлено для сенсора " + actionDetail.getActionSensorId());
                log.info("✅ Действие успешно отправлено для сенсора {}", actionDetail.getActionSensorId());
            } catch (Exception e) {
                System.out.println("    ❌ ОШИБКА ПРИ ОТПРАВКЕ ДЕЙСТВИЯ: " + e.getMessage());
                log.error("❌ Ошибка при отправке действия для сенсора {} в сценарии '{}': {}",
                        actionDetail.getActionSensorId(), scenarioName, e.getMessage(), e);
            }
        }

        System.out.println("  ✅ Все действия сценария '" + scenarioName + "' обработаны");
        log.info("✅ Все действия сценария '{}' обработаны", scenarioName);
    }

    private DeviceActionProto convertToDeviceActionProto(ScenarioProjection actionDetail) {
        DeviceActionProto.Builder builder = DeviceActionProto.newBuilder()
                .setSensorId(actionDetail.getActionSensorId())
                .setType(convertActionType(actionDetail.getActionType()));

        if (actionDetail.getActionValue() != null) {
            builder.setValue(actionDetail.getActionValue());
        }

        System.out.println("    🔄 Преобразовано в DeviceActionProto:");
        System.out.println("      sensorId=" + actionDetail.getActionSensorId());
        System.out.println("      type=" + actionDetail.getActionType());
        System.out.println("      value=" + actionDetail.getActionValue());

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
            System.out.println("    ❌ НЕИЗВЕСТНЫЙ ТИП ДЕЙСТВИЯ: " + actionType);
            log.error("❌ Неизвестный тип действия: {}", actionType);
            throw e;
        }
    }
}