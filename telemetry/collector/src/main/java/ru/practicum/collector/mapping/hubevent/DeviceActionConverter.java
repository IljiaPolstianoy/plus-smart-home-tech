package ru.practicum.collector.mapping.hubevent;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;

import java.util.ArrayList;
import java.util.List;

@Component
public class DeviceActionConverter {

    public List<DeviceActionAvro> toAvro(List<DeviceActionProto> protoList) {
        System.out.println("=== START ACTIONS CONVERSION ===");
        System.out.println("Number of actions: " + protoList.size());

        List<DeviceActionAvro> result = new ArrayList<>();
        for (int i = 0; i < protoList.size(); i++) {
            System.out.println("Converting action " + i);
            DeviceActionProto proto = protoList.get(i);

            try {
                DeviceActionAvro avro = toAvro(proto);
                result.add(avro);
                System.out.println("Action " + i + " converted successfully");
            } catch (Exception e) {
                System.err.println("ERROR converting action " + i + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("=== END ACTIONS CONVERSION ===");
        return result;
    }

    public DeviceActionAvro toAvro(DeviceActionProto proto) {
        System.out.println("Converting action for sensor: " + proto.getSensorId());
        System.out.println("Action type: " + proto.getType());

        DeviceActionAvro.Builder builder = DeviceActionAvro.newBuilder()
                .setSensorId(proto.getSensorId())
                .setType(toAvroActionType(proto.getType()));

        // ЗАМЕНИТЕ hasValue() на безопасную проверку:
        try {
            // Пробуем получить значение - если не установлено, будет исключение
            int value = proto.getValue();
            builder.setValue(value);
            System.out.println("Value set: " + value);
        } catch (Exception e) {
            // Поле value не установлено - это нормально для optional поля
            System.out.println("Value not set - using default");
        }

        DeviceActionAvro result = builder.build();
        System.out.println("Action conversion completed");
        return result;
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