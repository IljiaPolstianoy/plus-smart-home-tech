package ru.yandex.practicum.handler;

import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

public interface HandlerEvent {

    void handler(SensorsSnapshotAvro snapshotAvro);
}