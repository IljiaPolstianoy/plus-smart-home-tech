package ru.yandex.practicum.service;

import ru.yandex.practicum.shopping.ProductInCat;
import ru.yandex.practicum.shopping.ShoppingCartDto;

import java.util.Optional;

public interface ShoppingCartService {

    Optional<ShoppingCartDto> findShoppingCart(String userName);

    Optional<ShoppingCartDto> addProductsInShoppingCart(String userName, ProductInCat productInCat);

    boolean removeShoppingCart(String userName);

    Optional<ShoppingCartDto> removeProductsInShoppingCart(String userName, String productName);

    Optional<ShoppingCartDto> changeQuantityInShoppingCart(String userName, ProductInCat productInCat);

}
