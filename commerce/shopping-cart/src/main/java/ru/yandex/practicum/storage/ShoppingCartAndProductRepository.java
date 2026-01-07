package ru.yandex.practicum.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.model.shopping.ShoppingCartAndProduct;
import ru.yandex.practicum.model.shopping.ShoppingCartAndProductId;

import java.util.List;
import java.util.Optional;

public interface ShoppingCartAndProductRepository extends JpaRepository<ShoppingCartAndProduct, ShoppingCartAndProductId> {

    @Query("SELECT scp FROM ShoppingCartAndProduct scp " +
            "WHERE scp.shoppingCart.shoppingCartId = :shoppingCartId")
    List<ShoppingCartAndProduct> findByShoppingCartId(@Param("shoppingCartId") String shoppingCartId);

    @Query("SELECT scp FROM ShoppingCartAndProduct scp " +
            "WHERE scp.shoppingCart.userName = :userName " +
            "AND scp.product.productId = :productId")
    Optional<ShoppingCartAndProduct> findByShoppingCartUserNameAndProductId(
            @Param("userName") String userName,
            @Param("productId") String productId);

    @Modifying
    @Query("DELETE FROM ShoppingCartAndProduct scp " +
            "WHERE scp.product.productId IN :productIds " +
            "AND scp.shoppingCart.userName = :userName")
    boolean removeAllFromCart(@Param("productIds") List<String> productIds,
                           @Param("userName") String userName);
}