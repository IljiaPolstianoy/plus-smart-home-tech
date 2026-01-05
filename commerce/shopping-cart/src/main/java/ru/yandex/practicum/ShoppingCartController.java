package ru.yandex.practicum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.feign.ShoppingCartFeignClient;
import ru.yandex.practicum.model.quantity.ChangeProductQuantityRequest;
import ru.yandex.practicum.model.shopping.ShoppingCartDto;
import ru.yandex.practicum.service.ShoppingCartService;
import ru.yandex.practicum.validation.ExceptionType;
import ru.yandex.practicum.validation.NotNullOrBlank;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/shopping-cart")
@RequiredArgsConstructor
@Validated
public class ShoppingCartController implements ShoppingCartFeignClient {

    private final ShoppingCartService shoppingCartService;

    @GetMapping
    public Optional<ShoppingCartDto> getShoppingCart(@RequestParam @NotNullOrBlank(
            message = "Имя пользователя не должно быть пустым",
            exceptionType = ExceptionType.NOT_AUTHORIZED
    ) final String userName) {
        return shoppingCartService.findShoppingCart(userName);
    }

    @PutMapping
    public Optional<ShoppingCartDto> addProductsInShoppingCart(
            @RequestParam @NotNullOrBlank(
                    message = "Имя пользователя не должно быть пустым",
                    exceptionType = ExceptionType.NOT_AUTHORIZED
            ) final String userName,
            @RequestBody @Valid final List<ChangeProductQuantityRequest> changeProductQuantityRequests) {
        return shoppingCartService.addProductsInShoppingCart(userName, changeProductQuantityRequests);
    }

    @DeleteMapping
    public boolean deleteShoppingCart(@RequestParam @NotNullOrBlank(
            message = "Имя пользователя не должно быть пустым",
            exceptionType = ExceptionType.NOT_AUTHORIZED
    ) final String userName) {
        return shoppingCartService.removeShoppingCart(userName);
    }

    @PostMapping("/remove")
    public Optional<ShoppingCartDto> removeProductsInShoppingCart(
            @RequestParam @NotNullOrBlank(
                    message = "Имя пользователя не должно быть пустым",
                    exceptionType = ExceptionType.NOT_AUTHORIZED
            ) final String userName,
            @RequestBody @NotBlank final List<String> productName
    ) {
        return shoppingCartService.removeProductsInShoppingCart(userName, productName);
    }

    @PostMapping("/change-quantity")
    public Optional<ShoppingCartDto> changeQuantityInShoppingCart(
            @RequestParam @NotNullOrBlank(
                    message = "Имя пользователя не должно быть пустым",
                    exceptionType = ExceptionType.NOT_AUTHORIZED
            ) String userName,
            @RequestBody @Valid final ChangeProductQuantityRequest changeProductQuantityRequest
    ) {
        return shoppingCartService.changeQuantityInShoppingCart(userName, changeProductQuantityRequest);
    }
}