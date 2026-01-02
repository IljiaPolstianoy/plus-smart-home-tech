package ru.yandex.practicum.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.model.warehous.ProductQuantity;

import java.util.Optional;

public interface ProductQuantityRepository extends JpaRepository<ProductQuantity, Long> {

    Optional<ProductQuantity> findByProductQuantityId(String productQuantityId);

}
