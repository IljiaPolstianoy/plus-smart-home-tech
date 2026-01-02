package ru.yandex.practicum;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.feign.ShoppingStoreFeignClient;
import ru.yandex.practicum.model.Pageable;
import ru.yandex.practicum.model.product.ProductCategory;
import ru.yandex.practicum.model.product.ProductDto;
import ru.yandex.practicum.model.quantity.SetProductQuantityStateRequest;

@RestController
@RequestMapping("/api/v1/shopping-store")
@RequiredArgsConstructor
@Validated
public class ShoppingStoreController implements ShoppingStoreFeignClient {

    private final ShoppingStoreService shoppingStoreService;

    @GetMapping
    public Page<ProductDto> getProducts(
            @Valid @ModelAttribute Pageable pageable,
            @RequestBody ProductCategory category
    ) {
        return shoppingStoreService.getProducts(pageable, category);
    }

    @PutMapping
    public ProductDto createProduct(@Valid @RequestBody ProductDto productDto) {
        return shoppingStoreService.createProduct(productDto);
    }

    @PostMapping
    public ProductDto updateProduct(@Valid @RequestBody ProductDto productDto) {
        return shoppingStoreService.updateProduct(productDto);
    }

    @PostMapping("/removeProductFromStore")
    public boolean removeProductFromStore(@NotNull @RequestBody String productId) {
        return shoppingStoreService.deleteProduct(productId);
    }

    @PostMapping("/quantityState")
    public boolean updateQuantityState(@NotNull @RequestBody SetProductQuantityStateRequest setProductQuantityStateRequest) {
        return shoppingStoreService.updateQuantityStateProduct(setProductQuantityStateRequest);
    }

    @GetMapping("/{productId}")
    public ProductDto getProduct(@NotNull @PathVariable String productId) {
        return shoppingStoreService.getProduct(productId);
    }
}