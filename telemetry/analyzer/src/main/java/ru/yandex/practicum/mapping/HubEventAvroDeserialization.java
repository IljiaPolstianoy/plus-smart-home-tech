package ru.yandex.practicum.mapping;

import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.kafka.common.serialization.Deserializer;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

public class HubEventAvroDeserialization implements Deserializer<HubEventAvro> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public HubEventAvro deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            DatumReader<HubEventAvro> datumReader = new SpecificDatumReader<>(HubEventAvro.class);
            Decoder decoder = DecoderFactory.get().binaryDecoder(inputStream, null);
            return datumReader.read(null, decoder);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка десериализации HubEventAvro", e);
        }
    }

    @Override
    public void close() {
    }
}