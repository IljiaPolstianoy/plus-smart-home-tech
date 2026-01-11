package ru.yandex.practicum.model.delivery;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "delivery")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Delivery {
    @Id
    @Column(name = "delivery_id", nullable = false, length = Integer.MAX_VALUE)
    private String deliveryId;

    @Column(name = "total_volume", precision = 10, scale = 2)
    private BigDecimal totalVolume;

    @Column(name = "total_weight", precision = 10, scale = 2)
    private BigDecimal totalWeight;

    @Column(name = "fragile")
    private Boolean fragile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_from")
    private Address addressFrom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_to")
    private Address addressTo;

    @JoinColumn(name = "order_id")
    private String order;

    @Column(name = "state")
    private DeliveryState state;
}