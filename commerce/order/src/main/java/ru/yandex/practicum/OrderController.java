package ru.yandex.practicum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.model.order.CreateNewOrderRequest;
import ru.yandex.practicum.model.order.OrderDto;
import ru.yandex.practicum.model.order.ProductReturnRequest;
import ru.yandex.practicum.service.OrderService;

@RestController
@Slf4j
@RequestMapping("/api/v1/order")
@Validated
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public Page<OrderDto> findAllOrders(@RequestParam @NotNull final String userName) {
        return orderService.findAllOrders(userName);
    }

    @PutMapping
    public OrderDto createOrder(@Valid @RequestBody final CreateNewOrderRequest createNewOrderRequest) {
        return orderService.createOrder(createNewOrderRequest);
    }

    @PostMapping("/return")
    public OrderDto returnOrder(@Valid @RequestBody final ProductReturnRequest productReturnRequest) {
        return orderService.returnOrder(productReturnRequest);
    }

    @PostMapping("/payment")
    public OrderDto paymentOrder(@Valid @RequestBody final String orderId) {
        return orderService.paymentOrder(orderId);
    }

    @PostMapping("/payment/failed")
    public OrderDto paymentFailedOrder(@Valid @RequestBody final String orderId) {
        return orderService.paymentFailedOrder(orderId);
    }

    @PostMapping("/delivery")
    public OrderDto deliveryOrder(@Valid @RequestBody final String orderId) {
        return orderService.deliveryOrder(orderId);
    }

    @PostMapping("/deliver/failed")
    public OrderDto deliverFailedOrder(@Valid @RequestBody final String orderId) {
        return orderService.deliveryFailedOrder(orderId);
    }

    @PostMapping("/completed")
    public OrderDto completedOrder(@Valid @RequestBody final String orderId) {
        return orderService.completedOrder(orderId);
    }

    @PostMapping("/calculate/total")
    public OrderDto calculateTotal(@Valid @RequestBody final String orderId) {
        return orderService.calculateTotal(orderId);
    }

    @PostMapping("/calculate/delivery")
    public OrderDto calculateDeliveryOrder(@Valid @RequestBody final String orderId) {
        return orderService.calculateDeliveryOrder(orderId);
    }

    @PostMapping("/assembly")
    public OrderDto assemblyOrder(@Valid @RequestBody final String orderId) {
        return orderService.assemblyOrder(orderId);
    }

    @PostMapping("/assembly/failed")
    public OrderDto assemblyFailedOrder(@Valid @RequestBody final String orderId) {
        return orderService.assemblyFailedOrder(orderId);
    }
}