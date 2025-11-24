/*
package ru.practicum.collector;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.collector.model.hubevent.HubEvent;
import ru.practicum.collector.model.sensorevent.SensorEvent;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class CollectorController {

    private final SendKafka sendKafka;

    @PostMapping("/sensors")
    public boolean send(@Valid @RequestBody SensorEvent event) {
        return sendKafka.send(event);
    }

    @PostMapping("/hubs")
    public boolean send(@Valid @RequestBody HubEvent event) {
        return sendKafka.send(event);
    }
}
*/
