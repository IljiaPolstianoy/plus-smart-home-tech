package ru.practicum.collector;

import ru.practicum.collector.model.hubevent.HubEvent;
import ru.practicum.collector.model.sensorevent.SensorEvent;

public interface SendKafka {

    boolean send(SensorEvent event);

    boolean send(HubEvent event);
}