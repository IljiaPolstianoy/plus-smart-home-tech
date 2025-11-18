package ru.practicum.collector.mapping.hubevent;

import org.springframework.stereotype.Component;
import ru.practicum.collector.model.hubevent.ConditionType;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;

@Component
public class ConditionTypeConverter {

    public ConditionTypeAvro toAvro(ConditionType conditionType) {
        if (conditionType == null) {
            return null;
        }

        switch (conditionType) {
            case MOTION:
                return ConditionTypeAvro.MOTION;
            case LUMINOSITY:
                return ConditionTypeAvro.LUMINOSITY;
            case SWITCH:
                return ConditionTypeAvro.SWITCH;
            case TEMPERATURE:
                return ConditionTypeAvro.TEMPERATURE;
            case CO2LEVEL:
                return ConditionTypeAvro.CO2LEVEL;
            case HUMIDITY:
                return ConditionTypeAvro.HUMIDITY;
            default:
                throw new IllegalArgumentException("Unknown condition type: " + conditionType);
        }
    }
}
