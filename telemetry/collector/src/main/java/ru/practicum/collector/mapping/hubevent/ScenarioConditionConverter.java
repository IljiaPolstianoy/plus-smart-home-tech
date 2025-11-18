package ru.practicum.collector.mapping.hubevent;

import org.springframework.stereotype.Component;
import ru.practicum.collector.model.hubevent.ScenarioCondition;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;

import java.util.ArrayList;
import java.util.List;

@Component
public class ScenarioConditionConverter {

    private ConditionTypeConverter conditionTypeConverter;
    private ConditionOperationConverter conditionOperationConverter;

    public ScenarioConditionConverter(ConditionTypeConverter conditionTypeConverter, ConditionOperationConverter conditionOperationConverter) {
        this.conditionTypeConverter = conditionTypeConverter;
        this.conditionOperationConverter = conditionOperationConverter;
    }

    public List<ScenarioConditionAvro> toAvro(List<ScenarioCondition> scenarioConditions) {
        List<ScenarioConditionAvro> scenarioConditionAvros = new ArrayList<>();

        for (ScenarioCondition scenarioCondition : scenarioConditions) {
            scenarioConditionAvros.add(ScenarioConditionAvro.newBuilder()
                    .setSensorId(scenarioCondition.getSensorId())
                    .setType(conditionTypeConverter.toAvro(scenarioCondition.getType()))
                    .setOperation(conditionOperationConverter.toAvro(scenarioCondition.getOperation()))
                    .setValue(scenarioCondition.getValue())
                    .build());
        }

        return scenarioConditionAvros;
    }
}
