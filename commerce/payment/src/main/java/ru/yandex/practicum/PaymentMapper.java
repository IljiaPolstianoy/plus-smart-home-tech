package ru.yandex.practicum;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.model.payment.Payment;
import ru.yandex.practicum.model.payment.PaymentDto;

@Component
public class PaymentMapper {

    public static PaymentDto toPaymentDto(Payment payment) {
        return PaymentDto.builder()
                .paymentId(payment.getPaymentId())
                .totalPayment(payment.getTotalPayment())
                .deliveryTotal(payment.getDeliveryTotal())
                .freeTotal(payment.getFreeTotal())
                .build();
    }
}
