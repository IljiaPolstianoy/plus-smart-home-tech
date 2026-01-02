package ru.yandex.practicum.feign;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.model.Pageable;
import ru.yandex.practicum.model.product.ProductCategory;
import ru.yandex.practicum.model.product.ProductDto;
import ru.yandex.practicum.model.quantity.SetProductQuantityStateRequest;

@FeignClient(name = "shopping-store")
public interface ShoppingStoreFeignClient {

    @GetMapping("/api/v1/shopping-store")
    public Page<ProductDto> getProducts(
            @Valid @ModelAttribute Pageable pageable,
            @RequestBody ProductCategory category
    );

    @PutMapping("/api/v1/shopping-store")
    public ProductDto createProduct(@Valid @RequestBody ProductDto productDto);

    @PostMapping("/api/v1/shopping-store")
    public ProductDto updateProduct(@Valid @RequestBody ProductDto productDto);

    @PostMapping("/api/v1/shopping-store/removeProductFromStore")
    public boolean removeProductFromStore(@NotNull @RequestBody String productId);

    @PostMapping("/api/v1/shopping-store/quantityState")
    public boolean updateQuantityState(@NotNull @RequestBody SetProductQuantityStateRequest setProductQuantityStateRequest);

    @GetMapping("/api/v1/shopping-store/{productId}")
    public ProductDto getProduct(@NotNull @PathVariable String productId);
}
