package ru.practicum.collector.mapping.hubevent;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.ConditionOperationProto;
import ru.yandex.practicum.grpc.telemetry.event.ConditionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioConditionProto;
import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ScenarioConditionConverter {

    public List<ScenarioConditionAvro> toAvro(List<ScenarioConditionProto> protoList) {
        return protoList.stream()
                .map(this::toAvro)
                .collect(Collectors.toList());
    }

    public ScenarioConditionAvro toAvro(ScenarioConditionProto proto) {
        ScenarioConditionAvro.Builder builder = ScenarioConditionAvro.newBuilder()
                .setSensorId(proto.getSensorId())
                .setType(toAvroConditionType(proto.getType()))
                .setOperation(toAvroConditionOperation(proto.getOperation()));

        // Устанавливаем значение в зависимости от типа
        switch (proto.getValueCase()) {
            case BOOL_VALUE:
                builder.setValue(proto.getBoolValue());
                break;
            case INT_VALUE:
                builder.setValue(proto.getIntValue());
                break;
            case VALUE_NOT_SET:
            default:
                throw new IllegalArgumentException("Unsupported condition value type: " + proto.getValueCase());
        }

        return builder.build();
    }

    private ConditionTypeAvro toAvroConditionType(ConditionTypeProto proto) {
        return switch (proto) {
            case MOTION -> ConditionTypeAvro.MOTION;
            case LUMINOSITY -> ConditionTypeAvro.LUMINOSITY;
            case SWITCH -> ConditionTypeAvro.SWITCH;
            case TEMPERATURE -> ConditionTypeAvro.TEMPERATURE;
            case CO2LEVEL -> ConditionTypeAvro.CO2LEVEL;
            case HUMIDITY -> ConditionTypeAvro.HUMIDITY;
            default -> throw new IllegalArgumentException("Unknown condition type: " + proto);
        };
    }

    private ConditionOperationAvro toAvroConditionOperation(ConditionOperationProto proto) {
        return switch (proto) {
            case EQUALS -> ConditionOperationAvro.EQUALS;
            case GREATER_THAN -> ConditionOperationAvro.GREATER_THAN;
            case LOWER_THAN -> ConditionOperationAvro.LOWER_THAN;
            default -> throw new IllegalArgumentException("Unknown condition operation: " + proto);
        };
    }
}