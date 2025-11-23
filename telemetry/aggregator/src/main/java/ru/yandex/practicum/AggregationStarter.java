package ru.yandex.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.error.WakeupException;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.mapping.SensorEventDeserializer;
import ru.yandex.practicum.mapping.SensorSnapshotSerializer;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Класс AggregationStarter, ответственный за запуск агрегации данных.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private static final Duration CONSUME_ATTEMPT_TIMEOUT = Duration.ofMillis(1000);
    private static final List<String> TOPICS_INPUT = List.of("telemetry.sensors.v1");
    private static final String TOPIC_OUT = "telemetry.snapshots.v1";
    private final Map<String, SensorsSnapshotAvro> hubSnapshots = new ConcurrentHashMap<>();

    private static final Map<TopicPartition, OffsetAndMetadata> currentOffsets = new HashMap<>();

    /**
     * Метод для начала процесса агрегации данных.
     * Подписывается на топики для получения событий от датчиков,
     * формирует снимок их состояния и записывает в кафку.
     */
    public void start() {

        Properties configConsumer = getConsumerProperties();
        Properties configProducer = getProducerProperties();

        // создаём потребителя
        KafkaConsumer<String, SensorEventAvro> consumer = new KafkaConsumer<>(configConsumer);
        Producer<String, SensorsSnapshotAvro> producer = new KafkaProducer<>(configProducer);

        // регистрируем хук, в котором вызываем метод wakeup.
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {

            consumer.subscribe(TOPICS_INPUT);

            // Цикл обработки событий
            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(CONSUME_ATTEMPT_TIMEOUT);

                int count = 0;
                for (ConsumerRecord<String, SensorEventAvro> recordConsumer : records) {
                    // Обработка событий

                    Optional<SensorsSnapshotAvro> snapshotAvroOpt = updateState(recordConsumer.value());

                    if (snapshotAvroOpt.isPresent()) {
                        SensorsSnapshotAvro snapshotAvro = snapshotAvroOpt.get();
                        ProducerRecord<String, SensorsSnapshotAvro> recordProducer =
                                new ProducerRecord<>(TOPIC_OUT, snapshotAvro);
                        producer.send(recordProducer);
                        log.debug("Отправлен обновленный снапшот для хаба {}", snapshotAvro.getHubId());
                    }

                    manageOffsets(recordConsumer, count, consumer);
                    count++;
                }
                // фиксируем максимальный оффсет обработанных записей
                consumer.commitAsync();
            }

        } catch (WakeupException ignored) {
            // игнорируем - закрываем консьюмер и продюсер в блоке finally
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий от датчиков", e);
        } finally {

            try {
                // Перед тем, как закрыть продюсер и консьюмер, нужно убедится,
                // что все сообщения, лежащие в буффере, отправлены и
                // все оффсеты обработанных сообщений зафиксированы

                producer.flush();
                consumer.commitSync(currentOffsets);
            } finally {
                log.info("Закрываем консьюмер");
                consumer.close();
                log.info("Закрываем продюсер");
                producer.close();
            }
        }
    }

    private static void manageOffsets(ConsumerRecord<String, SensorEventAvro> record, int count, KafkaConsumer<String, SensorEventAvro> consumer) {
        // обновляем текущий оффсет для топика-партиции
        currentOffsets.put(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset() + 1)
        );

        if (count % 10 == 0) {
            consumer.commitAsync(currentOffsets, (offsets, exception) -> {
                if (exception != null) {
                    log.warn("Ошибка во время фиксации оффсетов: {}", offsets, exception);
                }
            });
        }
    }

    private static Properties getConsumerProperties() {
        Properties properties = new Properties();
        // Общие настройки
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, "SomeConsumer");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "some.group.id");
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, SensorEventDeserializer.class.getName());

        // Настройки, выбранные по условиям задачи
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        properties.put(ConsumerConfig.FETCH_MAX_BYTES_CONFIG, 3072000);
        properties.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 307200);
        return properties;
    }

    private static Properties getProducerProperties() {
        Properties config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, SensorSnapshotSerializer.class);

        return config;
    }

    private SensorsSnapshotAvro getSnapshot(SensorEventAvro sensorEvent) {
        // Создаем состояние датчика из события
        SensorStateAvro sensorState = createSensorState(sensorEvent);

        // Создаем карту состояний (в данном случае для одного датчика)
        Map<String, SensorStateAvro> sensorsState = new HashMap<>();
        sensorsState.put(sensorEvent.getId(), sensorState);

        return SensorsSnapshotAvro.newBuilder()
                .setHubId(sensorEvent.getHubId())
                .setTimestamp(sensorEvent.getTimestamp())
                .setSensorsState(sensorsState)
                .build();
    }

    private SensorStateAvro createSensorState(SensorEventAvro sensorEvent) {
        return SensorStateAvro.newBuilder()
                .setTimestamp(sensorEvent.getTimestamp())
                .setData(extractSensorData(sensorEvent))
                .build();
    }

    private Object extractSensorData(SensorEventAvro sensorEvent) {
        // В зависимости от типа payload, извлекаем данные датчика
        Object payload = sensorEvent.getPayload();

        if (payload instanceof ClimateSensorAvro) {
            return payload;
        } else if (payload instanceof LightSensorAvro) {
            return payload;
        } else if (payload instanceof MotionSensorAvro) {
            return payload;
        } else if (payload instanceof SwitchSensorAvro) {
            return payload;
        } else if (payload instanceof TemperatureSensorAvro) {
            return payload;
        } else {
            throw new IllegalArgumentException("Unsupported sensor type: " +
                    (payload != null ? payload.getClass().getSimpleName() : "null"));
        }

    }

    private Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        // Проверяем, есть ли снапшот для event.getHubId()
        String hubId = event.getHubId();

        // Если снапшот есть, то достаём его
        // Если нет, то создаём новый
        SensorsSnapshotAvro currentSnapshot = hubSnapshots.computeIfAbsent(hubId, k ->
                SensorsSnapshotAvro.newBuilder()
                        .setHubId(hubId)
                        .setTimestamp(event.getTimestamp())
                        .setSensorsState(new HashMap<>())
                        .build()
        );

        // Проверяем, есть ли в снапшоте данные для event.getId()
        String sensorId = event.getId();
        Map<String, SensorStateAvro> sensorsState = new HashMap<>(currentSnapshot.getSensorsState());
        SensorStateAvro oldState = sensorsState.get(sensorId);

        // Если данные есть, то достаём их в переменную oldState
        if (oldState != null) {
            // Проверка, если oldState.getTimestamp() произошёл позже, чем
            // event.getTimestamp() или oldState.getData() равен
            // event.getPayload(), то ничего обновлять не нужно
            if (oldState.getTimestamp().isAfter(event.getTimestamp())) {
                log.debug("Событие устарело для датчика {}: текущее время {}, событие время {}",
                        sensorId, oldState.getTimestamp(), event.getTimestamp());
                return Optional.empty();
            }

            // Проверяем, изменились ли данные
            if (isDataEqualDeep(oldState.getData(), event.getPayload())) {
                log.debug("Данные не изменились для датчика {}", sensorId);
                return Optional.empty();
            }
        }

        // если дошли до сюда, значит, пришли новые данные и
        // снапшот нужно обновить
        // Создаём экземпляр SensorStateAvro на основе данных события
        SensorStateAvro newSensorState = SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(extractSensorData(event))
                .build();

        // Добавляем полученный экземпляр в снапшот
        sensorsState.put(sensorId, newSensorState);

        // Обновляем таймстемп снапшота таймстемпом из события
        SensorsSnapshotAvro updatedSnapshot = SensorsSnapshotAvro.newBuilder()
                .setHubId(hubId)
                .setTimestamp(event.getTimestamp()) // используем время последнего события
                .setSensorsState(sensorsState)
                .build();

        // Обновляем кэш
        hubSnapshots.put(hubId, updatedSnapshot);

        // Возвращаем снапшот - Optional.of(snapshot)
        return Optional.of(updatedSnapshot);
    }

    private boolean isDataEqualDeep(Object oldData, Object newData) {
        if (oldData == null && newData == null) return true;
        if (oldData == null || newData == null) return false;
        if (!oldData.getClass().equals(newData.getClass())) return false;

        // Сравниваем в зависимости от типа сенсора
        if (oldData instanceof ClimateSensorAvro) {
            ClimateSensorAvro oldClimate = (ClimateSensorAvro) oldData;
            ClimateSensorAvro newClimate = (ClimateSensorAvro) newData;
            return oldClimate.getTemperatureC() == newClimate.getTemperatureC() &&
                    oldClimate.getHumidity() == newClimate.getHumidity() &&
                    oldClimate.getCo2Level() == newClimate.getCo2Level();
        } else if (oldData instanceof LightSensorAvro) {
            LightSensorAvro oldLight = (LightSensorAvro) oldData;
            LightSensorAvro newLight = (LightSensorAvro) newData;
            return oldLight.getLinkQuality() == newLight.getLinkQuality() &&
                    oldLight.getLuminosity() == newLight.getLuminosity();
        } else if (oldData instanceof MotionSensorAvro) {
            MotionSensorAvro oldMotion = (MotionSensorAvro) oldData;
            MotionSensorAvro newMotion = (MotionSensorAvro) newData;
            return oldMotion.getLinkQuality() == newMotion.getLinkQuality() &&
                    oldMotion.getMotion() == newMotion.getMotion() &&
                    oldMotion.getVoltage() == newMotion.getVoltage();
        } else if (oldData instanceof SwitchSensorAvro) {
            SwitchSensorAvro oldSwitch = (SwitchSensorAvro) oldData;
            SwitchSensorAvro newSwitch = (SwitchSensorAvro) newData;
            return oldSwitch.getStat() == newSwitch.getStat();
        } else if (oldData instanceof TemperatureSensorAvro) {
            TemperatureSensorAvro oldTemp = (TemperatureSensorAvro) oldData;
            TemperatureSensorAvro newTemp = (TemperatureSensorAvro) newData;
            return oldTemp.getTemperatureC() == newTemp.getTemperatureC() &&
                    oldTemp.getTemperatureF() == newTemp.getTemperatureF();
        }

        return oldData.equals(newData);
    }
}