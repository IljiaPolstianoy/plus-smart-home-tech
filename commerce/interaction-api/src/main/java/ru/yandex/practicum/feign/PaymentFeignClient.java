package ru.yandex.practicum.feign;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.model.order.OrderDto;
import ru.yandex.practicum.model.payment.PaymentDto;

import java.math.BigDecimal;

@FeignClient(name = "payment")
public interface PaymentFeignClient {

    @PostMapping
    PaymentDto createPayment(@RequestBody @Valid OrderDto orderDto);

    @PostMapping("/totalCost")
    BigDecimal getTotalCost(@RequestBody @Valid OrderDto orderDto);

    @PostMapping("/refund")
    void refund(@NotBlank @Valid String paymentId);

    @PostMapping("/productCost")
    BigDecimal getProductCost(@RequestBody @Valid OrderDto orderDto);

    @PostMapping("/failed")
    void failed(@NotBlank @Valid String paymentId);
}
