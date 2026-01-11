package ru.yandex.practicum.feign;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.model.delivery.DeliveryDto;
import ru.yandex.practicum.model.order.OrderDto;

import java.math.BigDecimal;

@FeignClient(name = "delivery")
public interface DeliveryFeignClient {

    @PutMapping
    DeliveryDto createDelivery(@RequestBody final DeliveryDto deliveryDto);

    @PostMapping("/successful")
    void successfulDelivery(@RequestBody final String deliveryId);

    @PostMapping("/picked")
    void pickedDelivery(final String deliveryId);

    @PostMapping("/failed")
    void failedDelivery(final String deliveryId);

    @PostMapping("/cost")
    BigDecimal cost(@RequestBody @Valid final OrderDto orderDto);
}
