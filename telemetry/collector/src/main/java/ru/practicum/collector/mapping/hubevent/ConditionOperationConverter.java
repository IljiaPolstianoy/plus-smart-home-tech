package ru.practicum.collector.mapping.hubevent;

import org.springframework.stereotype.Component;
import ru.practicum.collector.model.hubevent.ConditionOperation;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;

import static ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro.*;

@Component
public class ConditionOperationConverter {

    public ConditionOperationAvro toAvro(ConditionOperation conditionOperation) {
        if (conditionOperation == null) {
            return null;
        }

        switch (conditionOperation) {
            case EQUALS:
                return ConditionOperationAvro.EQUALS;
            case GREATER_THAN:
                return ConditionOperationAvro.GREATER_THAN;
            case LOWER_THAN:
                return ConditionOperationAvro.LOWER_THAN;
            default:
                throw new IllegalArgumentException("Unknown device type: " + conditionOperation);
        }
    }
}
