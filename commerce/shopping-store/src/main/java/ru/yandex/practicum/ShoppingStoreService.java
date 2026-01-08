package ru.yandex.practicum;

import org.springframework.data.domain.Page;
import ru.yandex.practicum.model.Pageable;
import ru.yandex.practicum.model.product.ProductCategory;
import ru.yandex.practicum.model.product.ProductDto;
import ru.yandex.practicum.model.quantity.SetProductQuantityStateRequest;

public interface ShoppingStoreService {

    Page<ProductDto> getProducts(Pageable pageable, ProductCategory category);

    ProductDto createProduct(ProductDto productDto);

    ProductDto updateProduct(ProductDto productDto);

    boolean deleteProduct(String productId);

    boolean updateQuantityStateProduct(SetProductQuantityStateRequest setProductQuantityStateRequest);

    ProductDto getProduct(String productId);
}
