package ru.yandex.practicum;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.product.ProductCategory;
import ru.yandex.practicum.product.ProductDto;

public interface ShoppingStoreRepository extends JpaRepository<ProductDto, Long> {

    Page<ProductDto> findAllByProductCategory(Pageable pageable, ProductCategory category);

    boolean deleteByProductId(String productId);

    ProductDto getProductDtoByProductId(String productId);
}
