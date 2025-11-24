package ru.yandex.practicum.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class HandlerEventImpl implements HandlerEvent {
    private final ScenarioRepository scenarioRepository;
    private final HubRouterClientService hubRouterClientService;

    @Override
    public void handler(SensorsSnapshotAvro snapshotAvro, String hubId) {
        // === ПРИНУДИТЕЛЬНОЕ ЛОГИРОВАНИЕ ДЛЯ GITHUB ===
        System.out.println("=== GITHUB_DEBUG_HANDLER_START ===");
        System.out.println("🎯 Обработка снапшота для хаба: " + hubId);

        if ("hub-1".equals(hubId)) {
            checkTemperatureScenario(snapshotAvro, hubId);
        }

        // Специальная проверка для температурного сценария
        if ("hub-1".equals(hubId)) {
            System.out.println("=== GITHUB_DEBUG_TEMPERATURE_SCENARIO ===");
            final List<ScenarioProjection> tempScenarios = scenarioRepository.findScenariosWithDetailsByHubId(hubId)
                    .stream()
                    .filter(s -> "Регулировка температуры (спальня)".equals(s.getScenarioName()))
                    .collect(Collectors.toList());

            System.out.println("🔍 Найдено температурных сценариев: " + tempScenarios.size());
            for (ScenarioProjection tempScenario : tempScenarios) {
                System.out.println("   - ID: " + tempScenario.getScenarioId() +
                        ", сенсор условия: " + tempScenario.getSensorId() +
                        ", тип условия: " + tempScenario.getConditionType() +
                        ", операция: " + tempScenario.getConditionOperation() +
                        ", значение: " + tempScenario.getConditionValue() +
                        ", сенсор действия: " + tempScenario.getActionSensorId() +
                        ", тип действия: " + tempScenario.getActionType() +
                        ", значение действия: " + tempScenario.getActionValue());
            }

            // Проверяем наличие температурного сенсора в снапшоте
            String tempSensorId = "2b0bb4c1-7cf2-475a-a17c-e5cb6239d6e5"; // ID из лога теста
            if (snapshotAvro.getSensorsState().containsKey(tempSensorId)) {
                System.out.println("✅ Температурный сенсор " + tempSensorId + " найден в снапшоте");
                SensorStateAvro tempSensorState = snapshotAvro.getSensorsState().get(tempSensorId);
                System.out.println("   Данные сенсора: " + tempSensorState.getData());
            } else {
                System.out.println("❌ Температурный сенсор " + tempSensorId + " НЕ найден в снапшоте");
                System.out.println("   Доступные сенсоры: " + snapshotAvro.getSensorsState().keySet());
            }
        }

        System.out.println("=== GITHUB_DEBUG_HANDLER ===");
        System.out.println("🎯 Обработка снапшота для хаба: " + hubId);
        System.out.println("📊 Сенсоры в снапшоте: " + snapshotAvro.getSensorsState().keySet());

        // Детально логируем каждый сенсор в снапшоте
        snapshotAvro.getSensorsState().forEach((sensorId, sensorState) -> {
            System.out.println("🔍 Сенсор " + sensorId + ": data=" + sensorState.getData());
            log.info("🔍 Сенсор {}: timestamp={}, data={}",
                    sensorId, sensorState.getTimestamp(), sensorState.getData());
        });

        if (snapshotAvro.getSensorsState() == null || snapshotAvro.getSensorsState().isEmpty()) {
            System.out.println("❌ Нет данных о сенсорах в снапшоте!");
            return;
        }

        final Map<String, SensorStateAvro> sensorStateAvroMap = snapshotAvro.getSensorsState();

        final List<ScenarioProjection> scenarios = scenarioRepository.findScenariosWithDetailsByHubId(hubId);

        // === ПРИНУДИТЕЛЬНОЕ ЛОГИРОВАНИЕ ДЛЯ GITHUB ===
        System.out.println("🔍 Найдено сценариев в БД для хаба " + hubId + ": " + scenarios.size());
        log.info("Обработка снапшота для хаба {}. Найдено записей: {}", hubId, scenarios.size());

        // Логируем каждый сценарий для GitHub
        for (ScenarioProjection scenario : scenarios) {
            System.out.println("📋 Сценарий: " + scenario.getScenarioName() +
                    ", условия: " + scenario.getConditionType() +
                    ", действия: " + scenario.getActionType());
        }

        // Группируем по ID сценария
        Map<Long, List<ScenarioProjection>> scenariosById = scenarios.stream()
                .collect(Collectors.groupingBy(ScenarioProjection::getScenarioId));

        for (Map.Entry<Long, List<ScenarioProjection>> entry : scenariosById.entrySet()) {
            Long scenarioId = entry.getKey();
            List<ScenarioProjection> scenarioDetails = entry.getValue();

            String scenarioName = scenarioDetails.get(0).getScenarioName();

            // === ПРИНУДИТЕЛЬНОЕ ЛОГИРОВАНИЕ ДЛЯ GITHUB ===
            System.out.println("=== GITHUB_DEBUG_SCENARIO ===");
            System.out.println("🔍 Проверяем сценарий '" + scenarioName + "' для хаба " + hubId);

            log.info("🔍 Проверяем сценарий '{}' для хаба {}", scenarioName, hubId);

            // Детально логируем условия сценария
            List<ScenarioProjection> conditions = scenarioDetails.stream()
                    .filter(detail -> detail.getConditionType() != null)
                    .collect(Collectors.toList());

            System.out.println("   Условия сценария '" + scenarioName + "':");
            log.info("   Условия сценария '{}':", scenarioName);
            for (ScenarioProjection condition : conditions) {
                System.out.println("     - Сенсор: " + condition.getSensorId() +
                        ", Тип: " + condition.getConditionType() +
                        ", Операция: " + condition.getConditionOperation() +
                        ", Значение: " + condition.getConditionValue());
                log.info("     - Сенсор: {}, Тип: {}, Операция: {}, Значение: {}",
                        condition.getSensorId(), condition.getConditionType(),
                        condition.getConditionOperation(), condition.getConditionValue());
            }

            // Детально логируем действия сценария
            List<ScenarioProjection> actions = scenarioDetails.stream()
                    .filter(detail -> detail.getActionType() != null && detail.getActionSensorId() != null)
                    .collect(Collectors.toList());

            System.out.println("   Действия сценария '" + scenarioName + "':");
            log.info("   Действия сценария '{}':", scenarioName);
            for (ScenarioProjection action : actions) {
                System.out.println("     - Сенсор: " + action.getActionSensorId() +
                        ", Тип: " + action.getActionType() +
                        ", Значение: " + action.getActionValue());
                log.info("     - Сенсор: {}, Тип: {}, Значение: {}",
                        action.getActionSensorId(), action.getActionType(), action.getActionValue());
            }

            boolean allConditionsMet = areAllConditionsMet(scenarioDetails, sensorStateAvroMap);

            // === ПРИНУДИТЕЛЬНОЕ ЛОГИРОВАНИЕ ДЛЯ GITHUB ===
            System.out.println("   Условия сценария '" + scenarioName + "' выполнены: " + allConditionsMet);
            log.info("   Условия сценария '{}' выполнены: {}", scenarioName, allConditionsMet);

            if (allConditionsMet) {
                System.out.println("✅ АКТИВАЦИЯ СЦЕНАРИЯ '" + scenarioName + "'");
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
            System.out.println("❌ Нет условий для проверки");
            log.warn("Нет условий для проверки");
            return false;
        }

        for (ScenarioProjection condition : conditions) {
            SensorStateAvro sensorState = sensorStates.get(condition.getSensorId());
            if (sensorState == null) {
                System.out.println("❌ Сенсор " + condition.getSensorId() + " не найден в снапшоте");
                log.warn("❌ Сенсор {} не найден в снапшоте", condition.getSensorId());
                return false;
            }

            boolean conditionMet = isConditionMet(condition, sensorState);
            System.out.println("   Проверка условия для сенсора " + condition.getSensorId() + ": " + conditionMet);
            log.info("   Проверка условия для сенсора {}: {}", condition.getSensorId(), conditionMet);

            if (!conditionMet) {
                return false;
            }
        }
        return true;
    }

    private boolean isConditionMet(ScenarioProjection condition, SensorStateAvro sensorState) {
        Object sensorData = sensorState.getData();
        System.out.println("   Данные сенсора " + condition.getSensorId() + ": " + sensorData);
        System.out.println("   Тип данных: " + (sensorData != null ? sensorData.getClass().getName() : "null"));
        log.info("   Данные сенсора {}: {}, тип: {}",
                condition.getSensorId(), sensorData,
                sensorData != null ? sensorData.getClass().getName() : "null");

        // Для отладки - выводим все поля через рефлексию
        if (sensorData != null) {
            try {
                java.lang.reflect.Field[] fields = sensorData.getClass().getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {
                    field.setAccessible(true);
                    System.out.println("      Поле " + field.getName() + ": " + field.get(sensorData));
                }
            } catch (Exception e) {
                System.out.println("      Ошибка рефлексии: " + e.getMessage());
            }
        }

        switch (condition.getConditionType()) {
            case "TEMPERATURE":
                System.out.println("=== GITHUB_DEBUG_TEMPERATURE_CONDITION ===");
                System.out.println("   Проверка температурного условия");
                System.out.println("   Сенсор: " + condition.getSensorId());
                System.out.println("   Операция: " + condition.getConditionOperation());
                System.out.println("   Ожидаемое значение: " + condition.getConditionValue());

                if (sensorData instanceof org.apache.avro.generic.GenericRecord) {
                    org.apache.avro.generic.GenericRecord record = (org.apache.avro.generic.GenericRecord) sensorData;

                    // Пробуем все возможные поля для температуры
                    String[] tempFields = {"temperatureC", "temperature", "temp", "value"};
                    Integer temperature = null;
                    String foundField = null;

                    for (String fieldName : tempFields) {
                        if (record.hasField(fieldName)) {
                            Object tempObj = record.get(fieldName);
                            System.out.println("   🔍 Поле '" + fieldName + "': " + tempObj);

                            if (tempObj instanceof Number) {
                                temperature = ((Number) tempObj).intValue();
                                foundField = fieldName;
                                System.out.println("   ✅ Найдена температура: " + temperature + "°C в поле '" + fieldName + "'");
                                break;
                            }
                        }
                    }

                    if (temperature != null) {
                        System.out.println("   🌡️ Фактическая температура: " + temperature + "°C");
                        System.out.println("   📋 Условие: " + condition.getConditionOperation() + " " + condition.getConditionValue() + "°C");

                        boolean result = checkNumericCondition(condition, temperature);
                        System.out.println("   📊 Результат проверки: " + result);
                        return result;
                    } else {
                        System.out.println("   ❌ Температура не найдена в данных");
                        System.out.println("   ❌ Доступные поля: " + record.getSchema().getFields().stream()
                                .map(org.apache.avro.Schema.Field::name)
                                .collect(Collectors.toList()));
                    }
                }
                break;

            case "MOTION":
                if (sensorData instanceof org.apache.avro.generic.GenericRecord) {
                    org.apache.avro.generic.GenericRecord record = (org.apache.avro.generic.GenericRecord) sensorData;
                    try {
                        Object motionObj = record.get("motion");
                        if (motionObj instanceof Boolean) {
                            boolean motion = (Boolean) motionObj;
                            System.out.println("   Движение: " + motion + ", условие: " +
                                    condition.getConditionOperation() + " " + condition.getConditionValue());
                            return checkBooleanCondition(condition, motion);
                        }
                    } catch (Exception e) {
                        System.out.println("   Ошибка получения движения: " + e.getMessage());
                    }
                }
                break;

            case "SWITCH":
                if (sensorData instanceof org.apache.avro.generic.GenericRecord) {
                    org.apache.avro.generic.GenericRecord record = (org.apache.avro.generic.GenericRecord) sensorData;
                    try {
                        Object switchObj = record.get("stat");
                        if (switchObj instanceof Boolean) {
                            boolean switchState = (Boolean) switchObj;
                            System.out.println("   Переключатель: " + switchState + ", условие: " +
                                    condition.getConditionOperation() + " " + condition.getConditionValue());
                            return checkBooleanCondition(condition, switchState);
                        }
                    } catch (Exception e) {
                        System.out.println("   Ошибка получения состояния переключателя: " + e.getMessage());
                    }
                }
                break;

            case "LUMINOSITY":
                if (sensorData instanceof org.apache.avro.generic.GenericRecord) {
                    org.apache.avro.generic.GenericRecord record = (org.apache.avro.generic.GenericRecord) sensorData;
                    try {
                        Object lumObj = record.get("luminosity");
                        if (lumObj instanceof Integer) {
                            int luminosity = (Integer) lumObj;
                            System.out.println("   Освещенность: " + luminosity + ", условие: " +
                                    condition.getConditionOperation() + " " + condition.getConditionValue());
                            return checkNumericCondition(condition, luminosity);
                        }
                    } catch (Exception e) {
                        System.out.println("   Ошибка получения освещенности: " + e.getMessage());
                    }
                }
                break;

            case "HUMIDITY":
            case "CO2LEVEL":
                if (sensorData instanceof org.apache.avro.generic.GenericRecord) {
                    org.apache.avro.generic.GenericRecord record = (org.apache.avro.generic.GenericRecord) sensorData;
                    try {
                        String fieldName = "HUMIDITY".equals(condition.getConditionType()) ? "humidity" : "co2Level";
                        Object valueObj = record.get(fieldName);
                        if (valueObj instanceof Integer) {
                            int value = (Integer) valueObj;
                            System.out.println("   " + condition.getConditionType() + ": " + value + ", условие: " +
                                    condition.getConditionOperation() + " " + condition.getConditionValue());
                            return checkNumericCondition(condition, value);
                        }
                    } catch (Exception e) {
                        System.out.println("   Ошибка получения " + condition.getConditionType() + ": " + e.getMessage());
                    }
                }
                break;

            default:
                System.out.println("❌ Неизвестный тип условия: " + condition.getConditionType());
                log.warn("❌ Неизвестный тип условия: {}", condition.getConditionType());
        }

        return false;
    }

    private boolean checkNumericCondition(ScenarioProjection condition, int sensorValue) {
        Integer conditionValue = condition.getConditionValue();
        if (conditionValue == null) {
            System.out.println("❌ Отсутствует значение условия для числовой проверки");
            log.warn("❌ Отсутствует значение условия для числовой проверки");
            return false;
        }

        // Преобразуем операции к стандартным названиям
        String operation = condition.getConditionOperation();
        System.out.println("   Операция: " + operation + ", сенсор: " + sensorValue + ", условие: " + conditionValue);

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
                result = sensorValue == conditionValue;
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
                System.out.println("❌ Неизвестная операция: " + operation);
                log.warn("❌ Неизвестная операция: {}", operation);
                return false;
        }

        System.out.println("   Результат: " + sensorValue + " " + operation + " " + conditionValue + " = " + result);
        return result;
    }

    private boolean checkBooleanCondition(ScenarioProjection condition, boolean sensorValue) {
        if ("EQUALS".equals(condition.getConditionOperation())) {
            Integer conditionValue = condition.getConditionValue();
            boolean conditionBool = conditionValue != null && conditionValue != 0;
            boolean result = sensorValue == conditionBool;
            System.out.println("   " + sensorValue + " == " + conditionBool + " = " + result);
            log.info("   {} == {} = {}", sensorValue, conditionBool, result);
            return result;
        }
        System.out.println("❌ Неподдерживаемая операция для boolean: " + condition.getConditionOperation());
        log.warn("❌ Неподдерживаемая операция для boolean: {}", condition.getConditionOperation());
        return false;
    }

    private void activateScenario(Long scenarioId, String scenarioName, String hubId,
                                  List<ScenarioProjection> scenarioDetails) {
        // Фильтруем только действия (где есть actionType)
        List<ScenarioProjection> actions = scenarioDetails.stream()
                .filter(detail -> detail.getActionType() != null && detail.getActionSensorId() != null)
                .collect(Collectors.toList());

        System.out.println("🎯 Выполняем " + actions.size() + " действий для сценария '" + scenarioName + "'");
        log.info("🎯 Выполняем {} действий для сценария '{}'", actions.size(), scenarioName);

        for (ScenarioProjection actionDetail : actions) {
            System.out.println("   🚀 Действие: сенсор=" + actionDetail.getActionSensorId() +
                    ", тип=" + actionDetail.getActionType() +
                    ", значение=" + actionDetail.getActionValue());
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

    private void checkTemperatureScenario(SensorsSnapshotAvro snapshotAvro, String hubId) {
        System.out.println("=== GITHUB_DEBUG_TEMPERATURE_SCENARIO_CHECK ===");

        // 1. Проверяем наличие температурного сенсора в снапшоте
        String tempSensorId = "2b0bb4c1-7cf2-475a-a17c-e5cb6239d6e5";
        boolean hasTempSensor = snapshotAvro.getSensorsState().containsKey(tempSensorId);
        System.out.println("📊 Температурный сенсор " + tempSensorId + " в снапшоте: " + hasTempSensor);

        if (hasTempSensor) {
            var tempSensorState = snapshotAvro.getSensorsState().get(tempSensorId);
            System.out.println("🌡️ Данные температурного сенсора: " + tempSensorState.getData());
            System.out.println("📋 Тип данных: " + (tempSensorState.getData() != null ?
                    tempSensorState.getData().getClass().getName() : "null"));

            // Детальный анализ данных
            if (tempSensorState.getData() instanceof org.apache.avro.generic.GenericRecord) {
                org.apache.avro.generic.GenericRecord record =
                        (org.apache.avro.generic.GenericRecord) tempSensorState.getData();
                System.out.println("🔍 Схема Avro: " + record.getSchema().getFullName());
                System.out.println("📝 Все поля температурного сенсора:");
                for (org.apache.avro.Schema.Field field : record.getSchema().getFields()) {
                    Object value = record.get(field.name());
                    System.out.println("   - " + field.name() + ": " + value +
                            " (тип: " + (value != null ? value.getClass().getSimpleName() : "null") + ")");
                }
            }
        }

        // 2. Проверяем сценарий в БД
        final List<ScenarioProjection> scenarios = scenarioRepository.findScenariosWithDetailsByHubId(hubId);
        var tempScenarios = scenarios.stream()
                .filter(s -> "Регулировка температуры (спальня)".equals(s.getScenarioName()))
                .collect(Collectors.toList());

        System.out.println("📋 Найдено температурных сценариев: " + tempScenarios.size());
        for (var tempScenario : tempScenarios) {
            System.out.println("🎯 Детали сценария 'Регулировка температуры (спальня)':");
            System.out.println("   ID: " + tempScenario.getScenarioId());
            System.out.println("   Сенсор условия: " + tempScenario.getSensorId());
            System.out.println("   Тип условия: " + tempScenario.getConditionType());
            System.out.println("   Операция: " + tempScenario.getConditionOperation());
            System.out.println("   Значение: " + tempScenario.getConditionValue());
            System.out.println("   Сенсор действия: " + tempScenario.getActionSensorId());
            System.out.println("   Тип действия: " + tempScenario.getActionType());
            System.out.println("   Значение действия: " + tempScenario.getActionValue());
        }
    }
}