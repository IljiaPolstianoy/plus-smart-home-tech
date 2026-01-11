package ru.yandex.practicum.model.order;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Setter
@Getter
@ToString
@Builder(toBuilder = true)
@Entity
@Table(name = "orders")
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {

    @NotBlank
    @Id
    @Column(name = "order_id")
    private String orderId;

    @Column(name = "shopping_cart_id")
    private String shoppingCartId;

    @NotEmpty
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "orders_products",
            joinColumns = @JoinColumn(name = "order_id")
    )
    @MapKeyColumn(name = "product_id")
    @Column(name = "quantity")
    private Map<String, Long> products;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "delivery_id")
    private String deliveryId;

    @Column
    private State state;

    @Column(name = "delivery_weight")
    private BigDecimal deliveryWeight;

    @Column(name = "delivery_volume")
    private BigDecimal deliveryVolume;

    @Column(name = "fragile")
    private boolean fragile;

    @Column(name = "total_price")
    private BigDecimal totalPrice;

    @Column(name = "delivery_price")
    private BigDecimal deliveryPrice;

    @Column(name = "product_price")
    private BigDecimal productPrice;
}
