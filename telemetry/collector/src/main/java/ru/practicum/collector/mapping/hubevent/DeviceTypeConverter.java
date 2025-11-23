package ru.practicum.collector.mapping.hubevent;

import org.springframework.stereotype.Component;
import ru.practicum.collector.model.hubevent.DeviceType;
import ru.yandex.practicum.grpc.telemetry.event.DeviceTypeProto;
import ru.yandex.practicum.kafka.telemetry.event.DeviceTypeAvro;

@Component
public class DeviceTypeConverter {

    public DeviceTypeAvro toAvro(DeviceTypeProto deviceType) {
        if (deviceType == null) {
            return null;
        }

        switch (deviceType) {
            case MOTION_SENSOR:
                return DeviceTypeAvro.MOTION_SENSOR;
            case TEMPERATURE_SENSOR:
                return DeviceTypeAvro.TEMPERATURE_SENSOR;
            case LIGHT_SENSOR:
                return DeviceTypeAvro.LIGHT_SENSOR;
            case CLIMATE_SENSOR:
                return DeviceTypeAvro.CLIMATE_SENSOR;
            case SWITCH_SENSOR:
                return DeviceTypeAvro.SWITCH_SENSOR;
            default:
                throw new IllegalArgumentException("Unknown device type: " + deviceType);
        }
    }
}
