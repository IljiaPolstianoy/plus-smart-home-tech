package ru.yandex.practicum.shopping;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.yandex.practicum.product.ProductDto;

@Getter
@Setter
@Entity
@Table(name = "shopping_cart_and_product")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingCartAndProduct {
    @EmbeddedId
    private ShoppingCartAndProductId id;

    @MapsId("shoppingCartId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shopping_cart_id", nullable = false)
    private ShoppingCart shoppingCart;

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductDto product;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Long quantity;

}