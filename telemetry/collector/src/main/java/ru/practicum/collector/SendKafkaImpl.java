package ru.practicum.collector;

import jakarta.annotation.PreDestroy;
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

    private final Producer<String, SensorEventAvro> sensorEventProducer;
    private final Producer<String, HubEventAvro> hubEventProducer;

    public SendKafkaImpl(SensorEventProtoToAvroConverter converterSensor,
                         HubEventProtoToAvroConverter converterHub) {
        this.converterSensor = converterSensor;
        this.converterHub = converterHub;

        this.sensorEventProducer = createSensorEventProducer();
        this.hubEventProducer = createHubEventProducer();
    }

    @Override
    public boolean send(SensorEventProto event) {
        try {
            final SensorEventAvro avroEvent = converterSensor.toAvro(event);
            final ProducerRecord<String, SensorEventAvro> record =
                    new ProducerRecord<>("telemetry.sensors.v1", avroEvent);

            sensorEventProducer.send(record);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка отправки SensorEvent в Kafka", e);
        }
    }

    @Override
    public boolean send(HubEventProto event) {
        try {
            final HubEventAvro avroEvent = converterHub.toAvro(event);
            final ProducerRecord<String, HubEventAvro> record =
                    new ProducerRecord<>("telemetry.hubs.v1", avroEvent);

            hubEventProducer.send(record);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Ошибка отправки HubEvent в Kafka", e);
        }
    }

    @PreDestroy
    public void close() {
        // Закрываем producers при остановке приложения
        if (sensorEventProducer != null) {
            sensorEventProducer.close();
        }
        if (hubEventProducer != null) {
            hubEventProducer.close();
        }
    }

    private Producer<String, SensorEventAvro> createSensorEventProducer() {
        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, SensorEventAvroSerialization.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);

        return new KafkaProducer<>(config);
    }

    private Producer<String, HubEventAvro> createHubEventProducer() {
        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, HubEventAvroSerialization.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);

        return new KafkaProducer<>(config);
    }
}