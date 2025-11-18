package ru.practicum.collector.mapping.sensorevent;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.serialization.Serializer;
import ru.practicum.collector.error.SerializationException;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SensorEventAvroSerialization implements Serializer<SensorEventAvro> {
    private final EncoderFactory encoderFactory = EncoderFactory.get();

    @Override
    public byte[] serialize(String topic, SensorEventAvro event) {
        if (event == null) {
            return null;
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            BinaryEncoder encoder = encoderFactory.binaryEncoder(outputStream, null);
            DatumWriter<SensorEventAvro> datumWriter = new SpecificDatumWriter<>(SensorEventAvro.class);

            datumWriter.write(event, encoder);
            encoder.flush();

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Ошибка сериализации экземпляра SensorEventAvro", e);
        }
    }
}
