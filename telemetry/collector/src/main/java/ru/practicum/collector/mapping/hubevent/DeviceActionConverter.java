package ru.practicum.collector.mapping.hubevent;

import org.springframework.stereotype.Component;
import ru.practicum.collector.model.hubevent.DeviceAction;
import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;

import java.util.ArrayList;
import java.util.List;

@Component
public class DeviceActionConverter {

    private ActionTypeConverter actionTypeConverter;

    public DeviceActionConverter(ActionTypeConverter actionTypeConverter) {
        this.actionTypeConverter = actionTypeConverter;
    }

    public List<DeviceActionAvro> toAvro(List<DeviceAction> deviceActions) {
        if (deviceActions == null) {
            return null;
        }

        List<DeviceActionAvro> deviceActionAvros = new ArrayList<>();

        for (DeviceAction deviceAction : deviceActions) {
            deviceActionAvros.add(DeviceActionAvro.newBuilder()
                    .setSensorId(deviceAction.getSensorId())
                    .setType(actionTypeConverter.toAvro(deviceAction.getType()))
                    .setValue(deviceAction.getValue())
                    .build());
        }

        return deviceActionAvros;
    }
}
