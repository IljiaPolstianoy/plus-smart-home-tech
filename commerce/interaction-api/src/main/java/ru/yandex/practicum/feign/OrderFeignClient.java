package ru.yandex.practicum.feign;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.model.order.CreateNewOrderRequest;
import ru.yandex.practicum.model.order.OrderDto;
import ru.yandex.practicum.model.order.ProductReturnRequest;

@FeignClient(name = "order")
public interface OrderFeignClient {

    @GetMapping
    Page<OrderDto> findAllOrders(@RequestParam @NotNull final String userName);

    @PutMapping
    OrderDto createOrder(@Valid @RequestBody final CreateNewOrderRequest createNewOrderRequest);

    @PostMapping("/return")
    OrderDto returnOrder(@Valid @RequestBody final ProductReturnRequest productReturnRequest);

    @PostMapping("/payment")
    OrderDto paymentOrder(@Valid @RequestBody final String orderId);

    @PostMapping("/payment/failed")
    OrderDto paymentFailedOrder(@Valid @RequestBody final String orderId);

    @PostMapping("/delivery")
    OrderDto deliveryOrder(@Valid @RequestBody final String orderId);

    @PostMapping("/deliver/failed")
    OrderDto deliverFailedOrder(@Valid @RequestBody final String orderId);

    @PostMapping("/completed")
    OrderDto completedOrder(@Valid @RequestBody final String orderId);

    @PostMapping("/calculate/total")
    OrderDto calculateTotal(@Valid @RequestBody final String orderId);

    @PostMapping("/calculate/delivery")
    OrderDto calculateDeliveryOrder(@Valid @RequestBody final String orderId);

    @PostMapping("/assembly")
    OrderDto assemblyOrder(@Valid @RequestBody final String orderId);

    @PostMapping("/assembly/failed")
    OrderDto assemblyFailedOrder(@Valid @RequestBody final String orderId);
}
