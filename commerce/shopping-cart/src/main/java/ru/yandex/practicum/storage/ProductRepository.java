package ru.yandex.practicum.storage;

import org.springframework.data.repository.Repository;
import ru.yandex.practicum.model.product.ProductDto;

import java.util.Optional;

public interface ProductRepository extends Repository<ProductDto, Long> {


    Optional<ProductDto> findByProductId(String productId);
}
