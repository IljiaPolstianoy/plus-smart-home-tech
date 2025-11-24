package ru.practicum.collector.mapping.hubevent;

import com.google.protobuf.Timestamp;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;

@Component
public class HubEventProtoToAvroConverter {

    private final DeviceTypeConverter deviceTypeConverter;
    private final DeviceActionConverter deviceActionConverter;
    private final ScenarioConditionConverter scenarioConditionConverter;

    public HubEventProtoToAvroConverter(
            DeviceTypeConverter deviceTypeConverter,
            DeviceActionConverter deviceActionConverter,
            ScenarioConditionConverter scenarioConditionConverter
    ) {
        this.deviceTypeConverter = deviceTypeConverter;
        this.deviceActionConverter = deviceActionConverter;
        this.scenarioConditionConverter = scenarioConditionConverter;
    }

    public HubEventAvro toAvro(HubEventProto proto) {
        Object payload = convertPayload(proto);

        return HubEventAvro.newBuilder()
                .setHubId(proto.getHubId())
                .setTimestamp(convertToInstant(proto.getTimestamp())) // преобразование timestamp
                .setPayload(payload)
                .build();
    }

    private Object convertPayload(HubEventProto proto) {
        switch (proto.getPayloadCase()) {
            case DEVICE_ADDED:
                DeviceAddedEventProto deviceAdded = proto.getDeviceAdded();
                return DeviceAddedEventAvro.newBuilder()
                        .setId(deviceAdded.getId())
                        .setType(deviceTypeConverter.toAvro(deviceAdded.getType()))
                        .build();

            case DEVICE_REMOVED:
                DeviceRemovedEventProto deviceRemoved = proto.getDeviceRemoved();
                return DeviceRemovedEventAvro.newBuilder()
                        .setId(deviceRemoved.getId())
                        .build();

            case SCENARIO_ADDED:
                ScenarioAddedEventProto scenarioAdded = proto.getScenarioAdded();
                return ScenarioAddedEventAvro.newBuilder()
                        .setName(scenarioAdded.getName())
                        .setConditions(scenarioConditionConverter.toAvro(scenarioAdded.getConditionList()))
                        .setActions(deviceActionConverter.toAvro(scenarioAdded.getActionList()))
                        .build();

            case SCENARIO_REMOVED:
                ScenarioRemovedEventProto scenarioRemoved = proto.getScenarioRemoved();
                return ScenarioRemovedEventAvro.newBuilder()
                        .setName(scenarioRemoved.getName())
                        .build();

            case PAYLOAD_NOT_SET:
            default:
                throw new IllegalArgumentException("Unsupported payload type in HubEventProto: " + proto.getPayloadCase());
        }
    }

    private Instant convertToInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(
                timestamp.getSeconds(),
                timestamp.getNanos()
        );
    }
}