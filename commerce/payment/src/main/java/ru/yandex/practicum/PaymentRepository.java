package ru.yandex.practicum;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.model.payment.Payment;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByPaymentId(String paymentId);

    Optional<Payment> findByOrderId(String orderId);
}
