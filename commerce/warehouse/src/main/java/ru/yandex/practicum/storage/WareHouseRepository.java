package ru.yandex.practicum.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.model.warehous.ProductsInWarehouse;

import java.util.Optional;

public interface WareHouseRepository extends JpaRepository<ProductsInWarehouse, Long> {

    boolean checkByProductId(String productId);

    ProductsInWarehouse findByProductId(String productId);
}
