package ru.practicum.collector.mapping.hubevent;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DeviceActionConverter {

    public List<DeviceActionAvro> toAvro(List<DeviceActionProto> protoList) {
        return protoList.stream()
                .map(this::toAvro)
                .collect(Collectors.toList());
    }

    public DeviceActionAvro toAvro(DeviceActionProto proto) {
        DeviceActionAvro.Builder builder = DeviceActionAvro.newBuilder()
                .setSensorId(proto.getSensorId())
                .setType(toAvroActionType(proto.getType()));

        if (proto.hasValue()) {
            builder.setValue(proto.getValue());
        }

        return builder.build();
    }

    private ActionTypeAvro toAvroActionType(ActionTypeProto proto) {
        return switch (proto) {
            case ACTIVATE -> ActionTypeAvro.ACTIVATE;
            case DEACTIVATE -> ActionTypeAvro.DEACTIVATE;
            case INVERSE -> ActionTypeAvro.INVERSE;
            case SET_VALUE -> ActionTypeAvro.SET_VALUE;
            default -> throw new IllegalArgumentException("Unknown action type: " + proto);
        };
    }
}