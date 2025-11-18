package ru.practicum.collector.mapping.hubevent;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.serialization.Serializer;
import ru.practicum.collector.error.SerializationException;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class HubEventAvroSerialization implements Serializer<HubEventAvro> {

    @Override
    public byte[] serialize(String s, HubEventAvro event) {
        if (event == null) {
            return null;
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            BinaryEncoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
            DatumWriter<HubEventAvro> datumWriter = new SpecificDatumWriter<>(HubEventAvro.class);

            datumWriter.write(event, encoder);
            encoder.flush();

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Ошибка сериализации экземпляра HubEventAvro", e);
        }
    }
}
