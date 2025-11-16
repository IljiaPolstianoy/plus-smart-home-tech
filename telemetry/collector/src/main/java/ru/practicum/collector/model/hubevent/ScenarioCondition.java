package ru.practicum.collector.model.hubevent;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class ScenarioCondition {

    private String sensorId;

    private ConditionType type;

    private ConditionOperation operation;

    private int value;
}
