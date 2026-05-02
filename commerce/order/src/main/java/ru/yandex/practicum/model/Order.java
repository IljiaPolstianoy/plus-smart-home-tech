package ru.yandex.practicum.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import ru.yandex.practicum.model.shopping.ShoppingCart;
import ru.yandex.practicum.model.warehous.OrdersProduct;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @Column(name = "orders_id", nullable = false, length = Integer.MAX_VALUE)
    private String orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "shopping_cart_id")
    private ShoppingCart shoppingCart;

    @Size(max = 255)
    @Column(name = "payment_id")
    private String paymentId;

    @Size(max = 255)
    @Column(name = "delivery_id")
    private String deliveryId;

    @Column(name = "delivery_weight", precision = 10, scale = 2)
    private BigDecimal deliveryWeight;

    @Column(name = "delivery_volume", precision = 10, scale = 2)
    private BigDecimal deliveryVolume;

    @Column(name = "fragile")
    private Boolean fragile;

    @Column(name = "total_price", precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "delivery_price", precision = 10, scale = 2)
    private BigDecimal deliveryPrice;

    @Column(name = "product_price", precision = 10, scale = 2)
    private BigDecimal productPrice;
    @OneToMany(mappedBy = "order")
    private Set<OrdersProduct> ordersProducts = new LinkedHashSet<>();

    @Column(name = "state", columnDefinition = "order_state(0, 0)")
    private Object state;

}