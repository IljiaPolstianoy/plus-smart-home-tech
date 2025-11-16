package ru.practicum.collector.mapping.sensorevent;

import org.springframework.stereotype.Component;
import ru.practicum.collector.model.sensorevent.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

@Component
public class SensorEventToAvroConverter {

    public SensorEventAvro toAvro(SensorEvent event) {
        Object payload = convertPayload(event);

        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(payload)
                .build();
    }

    private Object convertPayload(SensorEvent event) {
        if (event instanceof LightSensorEvent) {
            LightSensorEvent lightEvent = (LightSensorEvent) event;
            return LightSensorAvro.newBuilder()
                    .setLinkQuality(lightEvent.getLinkQuality())
                    .setLuminosity(lightEvent.getLuminosity())
                    .build();

        } else if (event instanceof TemperatureSensorEvent) {
            TemperatureSensorEvent tempEvent = (TemperatureSensorEvent) event;
            return TemperatureSensorAvro.newBuilder()
                    .setId(tempEvent.getId())
                    .setHubId(tempEvent.getHubId())
                    .setTimestamp(tempEvent.getTimestamp())
                    .setTemperatureC(tempEvent.getTemperatureC())
                    .setTemperatureF(tempEvent.getTemperatureF())
                    .build();

        } else if (event instanceof SwitchSensorEvent) {
            SwitchSensorEvent switchEvent = (SwitchSensorEvent) event;
            return SwitchSensorAvro.newBuilder()
                    .setStat(switchEvent.isState())
                    .build();

        } else if (event instanceof ClimateSensorEvent) {
            ClimateSensorEvent climateEvent = (ClimateSensorEvent) event;
            return ClimateSensorAvro.newBuilder()
                    .setTemperatureC(climateEvent.getTemperatureC())
                    .setHumidity(climateEvent.getHumidity())
                    .setCo2Level(climateEvent.getCo2Level())
                    .build();

        } else if (event instanceof MotionSensorEvent) {
            MotionSensorEvent motionEvent = (MotionSensorEvent) event;
            return MotionSensorAvro.newBuilder()
                    .setLinkQuality(motionEvent.getLinkQuality())
                    .setMotion(motionEvent.isMotion())
                    .setVoltage(motionEvent.getVoltage())
                    .build();
        }

        throw new IllegalArgumentException("Unsupported sensor event type: " + event.getClass().getSimpleName());
    }
}