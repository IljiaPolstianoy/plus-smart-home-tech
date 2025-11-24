package ru.yandex.practicum.mapping;

import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

public class SensorSnapshotSerializer extends BaseAvroSerializer<SensorsSnapshotAvro> {
    public SensorSnapshotSerializer() {
        super(SensorsSnapshotAvro.getClassSchema());
    }
}
