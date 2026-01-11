package ru.yandex.practicum.model.warehous;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "booked_product")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(BookedProduct.BookedProductId.class)
public class BookedProduct {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private OrderBooking orderBooking;

    @Id
    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookedProductId implements Serializable {
        private String orderBooking;
        private String productId;
    }
}