package ru.yandex.practicum.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ScenarioWithDetails {
    private Long scenarioId;
    private String scenarioName;
    private String hubId;

    // Поля для условий
    private String sensorId;
    private String conditionType;
    private String conditionOperation;
    private Integer conditionValue;

    // Поля для действий
    private String actionType;
    private Integer actionValue;
    private String actionSensorId;

    public ScenarioWithDetails(Long scenarioId, String scenarioName, String hubId,
                               String sensorId, String conditionType, String conditionOperation, Integer conditionValue,
                               String actionType, Integer actionValue, String actionSensorId) {
        this.scenarioId = scenarioId;
        this.scenarioName = scenarioName;
        this.hubId = hubId;
        this.sensorId = sensorId;
        this.conditionType = conditionType;
        this.conditionOperation = conditionOperation;
        this.conditionValue = conditionValue;
        this.actionType = actionType;
        this.actionValue = actionValue;
        this.actionSensorId = actionSensorId;
    }
}