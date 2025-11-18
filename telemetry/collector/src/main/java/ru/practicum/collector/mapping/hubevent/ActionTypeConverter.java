package ru.practicum.collector.mapping.hubevent;

import org.springframework.stereotype.Component;
import ru.practicum.collector.model.hubevent.ActionType;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;

@Component
public class ActionTypeConverter {

    public ActionTypeAvro toAvro(ActionType actionType) {
        if (actionType == null) {
            return null;
        }

        switch (actionType) {
            case ACTIVATE:
                return ActionTypeAvro.ACTIVATE;
            case DEACTIVATE:
                return ActionTypeAvro.DEACTIVATE;
            case INVERSE:
                return ActionTypeAvro.INVERSE;
            case SET_VALUE:
                return ActionTypeAvro.SET_VALUE;
            default:
                throw new IllegalArgumentException("Unknown device type: " + actionType);
        }
    }
}
