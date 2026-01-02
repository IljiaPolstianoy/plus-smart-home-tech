package ru.yandex.practicum.feign;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.model.shopping.ProductInCat;
import ru.yandex.practicum.model.shopping.ShoppingCartDto;

import java.util.Optional;

@FeignClient(name = "shopping-cart")
public interface ShoppingCartFeignClient {

    @GetMapping("/api/v1/shopping-cart")
    public Optional<ShoppingCartDto> getShoppingCart(@RequestParam @NotBlank final String userName);

    @PutMapping("/api/v1/shopping-cart")
    public Optional<ShoppingCartDto> addProductsInShoppingCart(
            @RequestParam @NotBlank final String userName,
            @RequestBody @Valid final ProductInCat productInCat);

    @DeleteMapping("/api/v1/shopping-cart")
    public boolean deleteShoppingCart(@RequestParam @NotBlank final String userName);

    @PostMapping("/api/v1/shopping-cart/remove")
    public Optional<ShoppingCartDto> removeProductsInShoppingCart(
            @RequestParam @NotBlank final String userName,
            @RequestBody @NotBlank final String productName
    );

    @PostMapping("/api/v1/shopping-cart/change-quantity")
    public Optional<ShoppingCartDto> changeQuantityInShoppingCart(
            @RequestParam @NotBlank String userName,
            @RequestBody @Valid final ProductInCat productInCat
    );
}
