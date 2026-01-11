package ru.yandex.practicum.service;

import org.springframework.data.domain.Page;
import ru.yandex.practicum.model.order.CreateNewOrderRequest;
import ru.yandex.practicum.model.order.OrderDto;
import ru.yandex.practicum.model.order.ProductReturnRequest;

public interface OrderService {

    Page<OrderDto> findAllOrders(String userName);

    OrderDto createOrder(CreateNewOrderRequest createNewOrderRequest);

    OrderDto returnOrder(ProductReturnRequest productReturnRequest);

    OrderDto paymentOrder(String orderId);

    OrderDto paymentFailedOrder(String orderId);

    OrderDto deliveryOrder(String orderId);

    OrderDto deliveryFailedOrder(String orderId);

    OrderDto completedOrder(String orderId);

    OrderDto calculateTotal(String orderId);

    OrderDto calculateDeliveryOrder(String orderId);

    OrderDto assemblyOrder(String orderId);

    OrderDto assemblyFailedOrder(String orderId);
}
