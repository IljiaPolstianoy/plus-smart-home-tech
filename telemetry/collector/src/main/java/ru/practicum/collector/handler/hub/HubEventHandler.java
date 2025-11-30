package ru.practicum.collector.handler.hub;

import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;

public interface HubEventHandler {

    HubEventProto.PayloadCase getMessageType();

    void handler(SensorEventProto event);
}
