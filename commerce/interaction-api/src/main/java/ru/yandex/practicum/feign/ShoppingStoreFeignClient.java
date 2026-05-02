package ru.yandex.practicum.feign;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.model.product.ProductCategory;
import ru.yandex.practicum.model.product.ProductDto;
import ru.yandex.practicum.model.quantity.SetProductQuantityStateRequest;

import java.util.List;

@FeignClient(name = "shopping-store")
public interface ShoppingStoreFeignClient {

    @GetMapping("/api/v1/shopping-store")
    Page<ProductDto> getProducts(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) List<String> sort,
            @RequestParam(value = "category", required = false) ProductCategory category
    );

    @PutMapping("/api/v1/shopping-store")
    ProductDto createProduct(@Valid @RequestBody ProductDto productDto);

    @PostMapping("/api/v1/shopping-store")
    ProductDto updateProduct(@Valid @RequestBody ProductDto productDto);

    @PostMapping("/api/v1/shopping-store/removeProductFromStore")
    boolean removeProductFromStore(@NotNull @RequestBody String productId);

    @PostMapping("/api/v1/shopping-store/quantityState")
    boolean updateQuantityState(@NotNull @RequestBody SetProductQuantityStateRequest setProductQuantityStateRequest);

    @GetMapping("/api/v1/shopping-store/{productId}")
    ProductDto getProduct(@NotNull @PathVariable String productId);
}
