package ru.yandex.practicum.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.model.warehous.ProductsInWarehouse;

public interface WareHouseRepository extends JpaRepository<ProductsInWarehouse, Long> {
    boolean findByProductId(String productId);
}
