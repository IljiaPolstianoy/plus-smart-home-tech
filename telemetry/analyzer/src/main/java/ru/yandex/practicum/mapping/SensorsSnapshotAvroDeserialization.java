package ru.yandex.practicum.mapping;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

public class SensorsSnapshotAvroDeserialization implements Deserializer<SensorsSnapshotAvro> {

    private static final Logger log = LoggerFactory.getLogger(SensorsSnapshotAvroDeserialization.class);

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public SensorsSnapshotAvro deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            DatumReader<SensorsSnapshotAvro> datumReader = new SpecificDatumReader<>(SensorsSnapshotAvro.class);
            Decoder decoder = DecoderFactory.get().binaryDecoder(inputStream, null);
            return datumReader.read(null, decoder);
        } catch (IOException e) {
            log.error("Ошибка десериализации SensorsSnapshotAvro", e);
            throw new RuntimeException("Ошибка десериализации SensorsSnapshotAvro", e);
        }
    }

    @Override
    public void close() {
    }
}