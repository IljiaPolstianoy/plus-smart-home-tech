package ru.yandex.practicum.mapping;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Serializer;
import ru.practicum.collector.error.SerializationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BaseAvroSerializer<T extends SpecificRecordBase> implements Serializer<T> {

    private final EncoderFactory encoderFactory;
    private final DatumWriter<T> datumWriter;

    public BaseAvroSerializer(Schema schema) {
        this(EncoderFactory.get(), schema);
    }

    private BaseAvroSerializer(EncoderFactory encoderFactory, Schema schema) {
        this.encoderFactory = encoderFactory;
        this.datumWriter = new SpecificDatumWriter<>(schema);
    }

    @Override
    public byte[] serialize(String topic, T event) {
        if (event == null) {
            return null;
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            BinaryEncoder encoder = encoderFactory.binaryEncoder(outputStream, null);

            datumWriter.write(event, encoder);
            encoder.flush();

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Ошибка сериализации экземпляра SensorEventAvro", e);
        }
    }
}
