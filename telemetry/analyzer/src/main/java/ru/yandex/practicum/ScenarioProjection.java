package ru.yandex.practicum;

public interface ScenarioProjection {
    Long getScenarioId();
    String getScenarioName();
    String getHubId();
    String getSensorId();
    String getConditionType();
    String getConditionOperation();
    Integer getConditionValue();
    String getActionType();
    Integer getActionValue();
    String getActionSensorId();
}