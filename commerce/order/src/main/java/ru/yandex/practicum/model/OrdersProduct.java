/*
package ru.yandex.practicum.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import ru.yandex.practicum.model.order.OrderDto;

@Getter
@Setter
@Entity
@Table(name = "orders_products")
public class OrdersProduct {
    @EmbeddedId
    private OrdersProductId id;

    @MapsId("orderId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderDto order;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Long quantity;

}*/
