package ru.yandex.practicum.model.warehous;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "product_quantity")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductQuantity {

    @Id
    @Column(name = "product_quantity_id", nullable = false, length = Integer.MAX_VALUE)
    private String productQuantityId;

    @Column(name = "quantity")
    private Long quantity;

    @MapsId
    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_quantity_id", nullable = false, referencedColumnName = "product_id")
    private ProductsInWarehouse productsInWarehouse;

    public boolean addQuantity(Long quantity) {
         this.quantity += quantity;
         return true;
    }
}