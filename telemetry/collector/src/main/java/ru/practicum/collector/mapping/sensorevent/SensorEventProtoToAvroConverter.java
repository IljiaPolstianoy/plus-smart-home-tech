package ru.practicum.collector.mapping.sensorevent;

import com.google.protobuf.Timestamp;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;

@Component
public class SensorEventProtoToAvroConverter {

    public SensorEventAvro toAvro(SensorEventProto proto) {
        Object payload = convertPayload(proto);

        return SensorEventAvro.newBuilder()
                .setId(proto.getId())
                .setHubId(proto.getHubId())
                .setTimestamp(convertToInstant(proto.getTimestamp()))
                .setPayload(payload)
                .build();
    }

    private Object convertPayload(SensorEventProto proto) {
        switch (proto.getPayloadCase()) {
            case MOTION_SENSOR:
                MotionSensorProto motionSensor = proto.getMotionSensor();
                return MotionSensorAvro.newBuilder()
                        .setLinkQuality(motionSensor.getLinkQuality())
                        .setMotion(motionSensor.getMotion())
                        .setVoltage(motionSensor.getVoltage())
                        .build();

            case TEMPERATURE_SENSOR:
                TemperatureSensorProto tempSensor = proto.getTemperatureSensor();
                return TemperatureSensorAvro.newBuilder()
                        .setTemperatureC(tempSensor.getTemperatureC())
                        .setTemperatureF(tempSensor.getTemperatureF())
                        .build();

            case LIGHT_SENSOR:
                LightSensorProto lightSensor = proto.getLightSensor();
                return LightSensorAvro.newBuilder()
                        .setLinkQuality(lightSensor.getLinkQuality())
                        .setLuminosity(lightSensor.getLuminosity())
                        .build();

            case CLIMATE_SENSOR:
                ClimateSensorProto climateSensor = proto.getClimateSensor();
                return ClimateSensorAvro.newBuilder()
                        .setTemperatureC(climateSensor.getTemperatureC())
                        .setHumidity(climateSensor.getHumidity())
                        .setCo2Level(climateSensor.getCo2Level())
                        .build();

            case SWITCH_SENSOR:
                SwitchSensorProto switchSensor = proto.getSwitchSensor();
                return SwitchSensorAvro.newBuilder()
                        .setStat(switchSensor.getState())
                        .build();

            case PAYLOAD_NOT_SET:
            default:
                throw new IllegalArgumentException("Unsupported payload type in SensorEventProto: " + proto.getPayloadCase());
        }
    }

    private Instant convertToInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(
                timestamp.getSeconds(),
                timestamp.getNanos()
        );
    }
}