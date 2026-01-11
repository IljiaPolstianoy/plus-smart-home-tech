package ru.yandex.practicum.service;

import ru.yandex.practicum.model.delivery.DeliveryDto;
import ru.yandex.practicum.model.order.OrderDto;

import java.math.BigDecimal;

public interface DeliveryService {

    DeliveryDto createDelivery(DeliveryDto deliveryDto);

    void successfulDelivery(String deliveryId);

    void pickedDelivery(String deliveryId);

    void failedDelivery(String deliveryId);

    BigDecimal cost(OrderDto orderDto);
}
