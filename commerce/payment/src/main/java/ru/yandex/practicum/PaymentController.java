package ru.yandex.practicum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.model.order.OrderDto;
import ru.yandex.practicum.model.payment.PaymentDto;
import ru.yandex.practicum.service.PaymentService;

import java.math.BigDecimal;

@RestController
@Slf4j
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public PaymentDto createPayment(@RequestBody @Valid final OrderDto orderDto) {
        return paymentService.createPayment(orderDto);
    }

    @PostMapping("/totalCost")
    public BigDecimal getTotalCost(@RequestBody @Valid final OrderDto orderDto) {
        return paymentService.getTotalCost(orderDto);
    }

    @PostMapping("/refund")
    public void refund(@NotBlank @Valid final String orderId) {
        paymentService.refund(orderId);
    }

    @PostMapping("/productCost")
    public BigDecimal getProductCost(@RequestBody @Valid final OrderDto orderDto) {
        return paymentService.getProductCost(orderDto);
    }

    @PostMapping("/failed")
    public void failed(@NotBlank @Valid final String orderId) {
        paymentService.failed(orderId);
    }
}
