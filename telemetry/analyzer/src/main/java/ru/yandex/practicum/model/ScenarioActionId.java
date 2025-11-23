package ru.yandex.practicum.model;

import java.io.Serializable;
import java.util.Objects;

public class ScenarioActionId implements Serializable {
    private Long scenario;
    private String sensorId;
    private Long action;

    // конструктор по умолчанию
    public ScenarioActionId() {
    }

    public ScenarioActionId(Long scenario, String sensorId, Long action) {
        this.scenario = scenario;
        this.sensorId = sensorId;
        this.action = action;
    }

    // equals и hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScenarioActionId that = (ScenarioActionId) o;
        return Objects.equals(scenario, that.scenario) &&
                Objects.equals(sensorId, that.sensorId) &&
                Objects.equals(action, that.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scenario, sensorId, action);
    }
}