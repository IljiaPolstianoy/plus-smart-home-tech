package ru.practicum.collector;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Service;
import ru.practicum.collector.mapping.hubevent.HubEventAvroSerialization;
import ru.practicum.collector.mapping.hubevent.HubEventProtoToAvroConverter;
import ru.practicum.collector.mapping.sensorevent.SensorEventAvroSerialization;
import ru.practicum.collector.mapping.sensorevent.SensorEventProtoToAvroConverter;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.util.Properties;

@Service
public class SendKafkaImpl implements SendKafka {

    private final SensorEventProtoToAvroConverter converterSensor;

    private final HubEventProtoToAvroConverter converterHub;

    public SendKafkaImpl(SensorEventProtoToAvroConverter converterSensor, HubEventProtoToAvroConverter converterHub) {
        this.converterSensor = converterSensor;
        this.converterHub = converterHub;
    }

    @Override
    public boolean send(SensorEventProto event) {
        final Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, SensorEventAvroSerialization.class);

        final String topic = "telemetry.sensors.v1";

        try (Producer<String, SensorEventAvro> producer = new KafkaProducer<>(config)) {
            final SensorEventAvro avroEvent = converterSensor.toAvro(event);

            final ProducerRecord<String, SensorEventAvro> record = new ProducerRecord<>(topic, avroEvent);

            producer.send(record);

            return true;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка отправки в Kafka", e);
        }
    }

    @Override
    public boolean send(HubEventProto event) {
        final Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, HubEventAvroSerialization.class);

        final String topic = "telemetry.hubs.v1";

        try (Producer<String, HubEventAvro> producer = new KafkaProducer<>(config)) {
            final HubEventAvro avroEvent = converterHub.toAvro(event);

            final ProducerRecord<String, HubEventAvro> record = new ProducerRecord<>(topic, avroEvent);

            producer.send(record);

            return true;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка отправки в Kafka", e);
        }
    }
}
