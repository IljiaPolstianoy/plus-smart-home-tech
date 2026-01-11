package ru.yandex.practicum;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.model.delivery.DeliveryDto;
import ru.yandex.practicum.model.order.OrderDto;
import ru.yandex.practicum.service.DeliveryService;

import java.math.BigDecimal;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/vq/delivery")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PutMapping
    public DeliveryDto createDelivery(@RequestBody final DeliveryDto deliveryDto) {
        return deliveryService.createDelivery(deliveryDto);
    }

    @PostMapping("/successful")
    public void successfulDelivery(@RequestBody final String deliveryId) {
        deliveryService.successfulDelivery(deliveryId);
    }

    @PostMapping("/picked")
    public void pickedDelivery(final String deliveryId) {
        deliveryService.pickedDelivery(deliveryId);
    }

    @PostMapping("/failed")
    public void failedDelivery(final String deliveryId) {
        deliveryService.failedDelivery(deliveryId);
    }

    @PostMapping("/cost")
    public BigDecimal cost(@RequestBody @Valid final OrderDto orderDto) {
        return deliveryService.cost(orderDto);
    }
}
