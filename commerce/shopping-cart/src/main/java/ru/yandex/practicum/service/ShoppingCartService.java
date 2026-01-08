package ru.yandex.practicum.service;

import ru.yandex.practicum.model.quantity.ChangeProductQuantityRequest;
import ru.yandex.practicum.model.shopping.ShoppingCartDto;

import java.util.List;
import java.util.Optional;

public interface ShoppingCartService {

    Optional<ShoppingCartDto> findShoppingCart(String userName);

    Optional<ShoppingCartDto> addProductsInShoppingCart(String userName, List<ChangeProductQuantityRequest> changeProductQuantityRequests);

    boolean removeShoppingCart(String userName);

    Optional<ShoppingCartDto> removeProductsInShoppingCart(String userName, List<String> productName);

    Optional<ShoppingCartDto> changeQuantityInShoppingCart(String userName, ChangeProductQuantityRequest changeProductQuantityRequest);

}
