package ru.practicum.collector;

import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;

public interface SendKafka {

    boolean send(SensorEventProto event);

    boolean send(HubEventProto event);
}