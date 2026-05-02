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

import java.util.List;

@RestController
@RequestMapping("/api/v1/shopping-store")
@RequiredArgsConstructor
@Validated
public class ShoppingStoreController implements ShoppingStoreFeignClient {

    private final ShoppingStoreService shoppingStoreService;

    @GetMapping("/api/v1/shopping-store")
    public Page<ProductDto> getProducts(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            @RequestParam(value = "sort", required = false) List<String> sort,
            @RequestParam(value = "category", required = false) ProductCategory category
    ) {

        return shoppingStoreService.getProducts
                (Pageable.builder()
                                .page(page)
                                .size(size)
                                .sort(sort)
                                .build(),
                        category);
    }

    @PutMapping
    public ProductDto createProduct(@Valid @RequestBody final ProductDto productDto) {
        return shoppingStoreService.createProduct(productDto);
    }

    @PostMapping
    public ProductDto updateProduct(@Valid @RequestBody final ProductDto productDto) {
        return shoppingStoreService.updateProduct(productDto);
    }

    @PostMapping("/removeProductFromStore")
    public boolean removeProductFromStore(@NotNull @RequestBody final String productId) {
        return shoppingStoreService.deleteProduct(productId);
    }

    @PostMapping("/quantityState")
    public boolean updateQuantityState(@NotNull @RequestBody final SetProductQuantityStateRequest setProductQuantityStateRequest) {
        return shoppingStoreService.updateQuantityStateProduct(setProductQuantityStateRequest);
    }

    @GetMapping("/{productId}")
    public ProductDto getProduct(@NotNull @PathVariable final String productId) {
        return shoppingStoreService.getProduct(productId);
    }
}