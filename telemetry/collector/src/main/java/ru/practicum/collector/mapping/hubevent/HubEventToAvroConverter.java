package ru.practicum.collector.mapping.hubevent;

import org.springframework.stereotype.Component;
import ru.practicum.collector.model.hubevent.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Component
public class HubEventToAvroConverter {

    private DeviceTypeConverter deviceTypeConverter;
    private DeviceActionConverter deviceActionConverter;
    private ScenarioConditionConverter scenarioConditionConverter;

    public HubEventToAvroConverter(
            DeviceTypeConverter deviceTypeConverter,
            DeviceActionConverter deviceActionConverter,
            ScenarioConditionConverter scenarioConditionConverter
    ) {
        this.deviceTypeConverter = deviceTypeConverter;
        this.deviceActionConverter = deviceActionConverter;
        this.scenarioConditionConverter = scenarioConditionConverter;
    }

    public HubEventAvro toAvro(HubEvent event) {
        Object payload = convertPayload(event);

        return HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(payload)
                .build();
    }

    private Object convertPayload(HubEvent event) {
        if (event instanceof DeviceAddedEvent) {
            DeviceAddedEvent deviceAddedEvent = (DeviceAddedEvent) event;
            return DeviceAddedEventAvro.newBuilder()
                    .setId(deviceAddedEvent.getId())
                    .setType(deviceTypeConverter.toAvro(deviceAddedEvent.getDeviceType()))
                    .build();
        } else if (event instanceof DeviceRemovedEvent) {
            DeviceRemovedEvent deviceRemovedEvent = (DeviceRemovedEvent) event;
            return DeviceRemovedEventAvro.newBuilder()
                    .setId(deviceRemovedEvent.getId())
                    .build();
        } else if (event instanceof ScenarioAddedEvent) {
            ScenarioAddedEvent scenarioAddedEvent = (ScenarioAddedEvent) event;
            return ScenarioAddedEventAvro.newBuilder()
                    .setName(scenarioAddedEvent.getName())
                    .setConditions(scenarioConditionConverter.toAvro(scenarioAddedEvent.getConditions()))
                    .setActions(deviceActionConverter.toAvro(scenarioAddedEvent.getActions()))
                    .build();
        } else if (event instanceof ScenarioRemovedEvent) {
            ScenarioRemovedEvent scenarioRemovedEvent = (ScenarioRemovedEvent) event;
            return ScenarioRemovedEventAvro.newBuilder()
                    .setName(scenarioRemovedEvent.getName())
                    .build();
        }

        throw new IllegalArgumentException("Unsupported sensor event type: " + event.getClass().getSimpleName());
    }
}
