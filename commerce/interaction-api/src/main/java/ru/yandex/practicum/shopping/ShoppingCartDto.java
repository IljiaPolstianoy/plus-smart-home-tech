package ru.yandex.practicum.shopping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ShoppingCartDto {

    @NotBlank
    private String shoppingCartId;

    @NotNull
    private List<ProductInCat> shoppingCartItems;

    public boolean addProduct(ProductInCat productInCat) {
        return shoppingCartItems.add(productInCat);
    }
}
