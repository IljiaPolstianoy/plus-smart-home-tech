package ru.yandex.practicum.model.shopping;

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
    private List<ProductInCat> products;

    public boolean addProduct(List<ProductInCat> productInCats) {
        return products.addAll(productInCats);
    }
}
