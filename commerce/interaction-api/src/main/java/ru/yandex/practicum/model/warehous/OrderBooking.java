package ru.yandex.practicum.model.warehous;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "order_booking")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderBooking {

    @Id
    @Column(name = "order_booking_id")
    private String id;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    @Column(name = "delivery_id")
    private String deliveryId;

    @OneToMany(mappedBy = "orderBooking", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BookedProduct> bookedProducts = new HashSet<>();

    public void addProduct(String productId, Long quantity) {
        BookedProduct bookedProduct = BookedProduct.builder()
                .orderBooking(this)
                .productId(productId)
                .quantity(quantity)
                .build();
        this.bookedProducts.add(bookedProduct);
    }

    public void removeProduct(String productId) {
        bookedProducts.removeIf(bp -> bp.getProductId().equals(productId));
    }

    public Long getTotalQuantity() {
        return bookedProducts.stream()
                .mapToLong(BookedProduct::getQuantity)
                .sum();
    }
}