package ru.yandex.practicum.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.model.warehous.ProductsInWarehouse;

public interface WareHouseRepository extends JpaRepository<ProductsInWarehouse, Long> {

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM ProductsInWarehouse p WHERE p.productId = :productId")
    boolean checkByProductId(@Param("productId") String productId);

    @Query("SELECT p FROM ProductsInWarehouse p WHERE p.productId = :productId")
    ProductsInWarehouse findByProductId(@Param("productId") String productId);
}
