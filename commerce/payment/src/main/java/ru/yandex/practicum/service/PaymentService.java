package ru.yandex.practicum.service;

import ru.yandex.practicum.model.order.OrderDto;
import ru.yandex.practicum.model.payment.PaymentDto;

import java.math.BigDecimal;

public interface PaymentService {

    PaymentDto createPayment(OrderDto orderDto);

    BigDecimal getTotalCost(OrderDto orderDto);

    void refund(String orderId);

    BigDecimal getProductCost(OrderDto orderDto);

    void failed(String orderId);
}
