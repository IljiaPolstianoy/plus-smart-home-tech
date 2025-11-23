package ru.yandex.practicum.model;

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