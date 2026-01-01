package ru.yandex.practicum.warehous;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "products_in_warehouse")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductsInWarehouse {
    @Id
    @Column(name = "product_id", nullable = false, length = Integer.MAX_VALUE)
    private String productId;

    @NotNull
    @Column(name = "fragile", nullable = false)
    private boolean fragile = false;

    @NotNull
    @Column(name = "width", nullable = false, precision = 10, scale = 2)
    private BigDecimal width;

    @NotNull
    @Column(name = "height", nullable = false, precision = 10, scale = 2)
    private BigDecimal height;

    @NotNull
    @Column(name = "depth", nullable = false, precision = 10, scale = 2)
    private BigDecimal depth;

    @NotNull
    @Column(name = "weight", nullable = false, precision = 10, scale = 2)
    private BigDecimal weight;

}