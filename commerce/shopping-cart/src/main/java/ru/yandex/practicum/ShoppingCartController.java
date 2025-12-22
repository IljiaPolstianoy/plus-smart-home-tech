package ru.yandex.practicum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.service.ShoppingCartService;
import ru.yandex.practicum.shopping.ProductInCat;
import ru.yandex.practicum.shopping.ShoppingCartDto;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/shopping-cart")
@RequiredArgsConstructor
@Validated
public class ShoppingCartController {

    private final ShoppingCartService shoppingCartService;

    @GetMapping
    public Optional<ShoppingCartDto> getShoppingCart(@RequestParam @NotBlank final String userName) {
        return shoppingCartService.findShoppingCart(userName);
    }

    @PutMapping
    public Optional<ShoppingCartDto> addProductsInShoppingCart(
            @RequestParam @NotBlank final String userName,
            @RequestBody @Valid final ProductInCat productInCat) {
        return shoppingCartService.addProductsInShoppingCart(userName, productInCat);
    }

    @DeleteMapping
    public boolean deleteShoppingCart(@RequestParam @NotBlank final String userName) {
        return shoppingCartService.removeShoppingCart(userName);
    }

    @PostMapping("/remove")
    public Optional<ShoppingCartDto> removeProductsInShoppingCart(
            @RequestParam @NotBlank final String userName,
            @RequestBody @NotBlank final String productName
    ) {
        return shoppingCartService.removeProductsInShoppingCart(userName, productName);
    }

    @PostMapping("change-quantity")
    public Optional<ShoppingCartDto> changeQuantityInShoppingCart(
            @RequestParam @NotBlank String userName,
            @RequestBody @Valid final ProductInCat productInCat
    ) {
        return shoppingCartService.changeQuantityInShoppingCart(userName, productInCat);
    }
}