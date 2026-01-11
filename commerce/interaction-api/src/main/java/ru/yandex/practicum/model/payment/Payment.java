package ru.yandex.practicum.model.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "payment")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @Column(name = "payment_id", nullable = false, length = Integer.MAX_VALUE)
    private String paymentId;

    @Column(name = "total_product", precision = 10, scale = 2)
    private BigDecimal totalProduct;

    @Column(name = "delivery_total", precision = 10, scale = 2)
    private BigDecimal deliveryTotal;

    @Column(name = "total_payment", precision = 10, scale = 2)
    private BigDecimal totalPayment;

    @Column(name = "state")
    private State state;

    @Column(name = "free_total")
    private BigDecimal freeTotal;

    @Column(name = "order_id")
    private String orderId;
}