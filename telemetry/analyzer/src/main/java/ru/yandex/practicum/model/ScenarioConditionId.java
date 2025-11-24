package ru.yandex.practicum.model;

import java.io.Serializable;
import java.util.Objects;

public class ScenarioConditionId implements Serializable {
    private Long scenario;
    private String sensorId;
    private Long condition;

    // конструктор по умолчанию
    public ScenarioConditionId() {
    }

    public ScenarioConditionId(Long scenario, String sensorId, Long condition) {
        this.scenario = scenario;
        this.sensorId = sensorId;
        this.condition = condition;
    }

    // equals и hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScenarioConditionId that = (ScenarioConditionId) o;
        return Objects.equals(scenario, that.scenario) &&
                Objects.equals(sensorId, that.sensorId) &&
                Objects.equals(condition, that.condition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scenario, sensorId, condition);
    }
}