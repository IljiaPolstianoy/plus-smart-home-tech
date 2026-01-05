package ru.yandex.practicum.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.model.shopping.ShoppingCartAndProduct;

import java.util.List;
import java.util.Optional;

public interface ShoppingCartAndProductRepository extends JpaRepository<ShoppingCartAndProduct, String> {

    List<ShoppingCartAndProduct> findByShoppingCart_ShoppingCartId(String shoppingCartId);

    boolean removeAllShoppingCartAndProductByProduct_ProductIdInAndShoppingCart_UserName(List<String> productName, String userName);

    Optional<ShoppingCartAndProduct> findByShoppingCart_UserNameAndProduct_ProductId(String userName, String productId);
}
