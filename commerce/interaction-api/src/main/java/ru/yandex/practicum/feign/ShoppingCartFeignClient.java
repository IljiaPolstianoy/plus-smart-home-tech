package ru.yandex.practicum.feign;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.model.quantity.ChangeProductQuantityRequest;
import ru.yandex.practicum.model.shopping.ShoppingCartDto;
import ru.yandex.practicum.validation.ExceptionType;
import ru.yandex.practicum.validation.NotNullOrBlank;

import java.util.List;
import java.util.Optional;

@FeignClient(name = "shopping-cart")
public interface ShoppingCartFeignClient {

    @GetMapping("/api/v1/shopping-cart")
    Optional<ShoppingCartDto> getShoppingCart(@RequestParam @NotNullOrBlank(
            message = "Имя пользователя не должно быть пустым",
            exceptionType = ExceptionType.NOT_AUTHORIZED
    ) final String userName);

    @PutMapping("/api/v1/shopping-cart")
    Optional<ShoppingCartDto> addProductsInShoppingCart(
            @RequestParam @NotNullOrBlank(
                    message = "Имя пользователя не должно быть пустым",
                    exceptionType = ExceptionType.NOT_AUTHORIZED
            ) final String userName,
            @RequestBody @Valid final List<ChangeProductQuantityRequest> changeProductQuantityRequests);

    @DeleteMapping("/api/v1/shopping-cart")
    boolean deleteShoppingCart(@RequestParam @NotNullOrBlank(
            message = "Имя пользователя не должно быть пустым",
            exceptionType = ExceptionType.NOT_AUTHORIZED
    ) final String userName);

    @PostMapping("/api/v1/shopping-cart/remove")
    Optional<ShoppingCartDto> removeProductsInShoppingCart(
            @RequestParam @NotNullOrBlank(
                    message = "Имя пользователя не должно быть пустым",
                    exceptionType = ExceptionType.NOT_AUTHORIZED
            ) final String userName,
            @RequestBody @NotBlank final List<String> productName
    );

    @PostMapping("/api/v1/shopping-cart/change-quantity")
    Optional<ShoppingCartDto> changeQuantityInShoppingCart(
            @RequestParam @NotNullOrBlank(
                    message = "Имя пользователя не должно быть пустым",
                    exceptionType = ExceptionType.NOT_AUTHORIZED
            ) String userName,
            @RequestBody @Valid final ChangeProductQuantityRequest changeProductQuantityRequest
    );
}
