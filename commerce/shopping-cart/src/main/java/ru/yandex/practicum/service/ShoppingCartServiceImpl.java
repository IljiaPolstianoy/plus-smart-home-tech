package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.error.ErrorWhenDeletingAnItemFromTheShoppingCart;
import ru.yandex.practicum.error.ShoppingCartDeactivate;
import ru.yandex.practicum.product.ProductDto;
import ru.yandex.practicum.shopping.*;
import ru.yandex.practicum.storage.ProductRepository;
import ru.yandex.practicum.storage.ShoppingCartAndProductRepository;
import ru.yandex.practicum.storage.ShoppingCartRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private final ShoppingCartRepository shoppingCartRepository;
    private final ShoppingCartAndProductRepository shoppingCartAndProductRepository;
    private final ProductRepository productRepository;

    @Override
    public Optional<ShoppingCartDto> findShoppingCart(final String userName) {

        final ShoppingCart shoppingCart = getShoppingCart(userName);

        final List<ShoppingCartAndProduct> shoppingCartAndProducts = shoppingCartAndProductRepository.findByShoppingCart_ShoppingCartId(shoppingCart.getShoppingCartId());

        final List<ProductInCat> products = new ArrayList<>();

        for (ShoppingCartAndProduct shoppingCartAndProduct : shoppingCartAndProducts) {
            final ProductInCat product = new ProductInCat();
            product.setProductId(shoppingCartAndProduct.getProduct().getProductId());
            product.setQuantity(product.getQuantity());
            products.add(product);
        }
        return Optional.of(ShoppingCartDto.builder()
                .shoppingCartId(shoppingCart.getShoppingCartId())
                .shoppingCartItems(products)
                .build());
    }

    @Override
    public Optional<ShoppingCartDto> addProductsInShoppingCart(String userName, ProductInCat productInCat) {

        final ShoppingCart shoppingCartDto = getShoppingCart(userName);

        if (shoppingCartDto.getShoppingCartState().equals(ShoppingCartState.DELIVERED)) {
            throw new ShoppingCartDeactivate("Корзина с id " + shoppingCartDto.getShoppingCartId() + " неактивна");
        }

        final ProductDto productDto = productRepository.findByProductId(productInCat.getProductId()).orElse(null);

        final ShoppingCartAndProduct shoppingCartAndProductNew = ShoppingCartAndProduct.builder()
                .shoppingCart(shoppingCartDto)
                .product(productDto)
                .quantity(productInCat.getQuantity())
                .build();

        shoppingCartAndProductRepository.save(shoppingCartAndProductNew);

        return findShoppingCart(userName);
    }

    @Override
    public boolean removeShoppingCart(String userName) {

        final ShoppingCart shoppingCart = getShoppingCart(userName);

        shoppingCart.setShoppingCartState(ShoppingCartState.DELIVERED);
        shoppingCartRepository.save(shoppingCart);

        return true;
    }

    @Override
    public Optional<ShoppingCartDto> removeProductsInShoppingCart(String userName, String productName) {
        if (!shoppingCartAndProductRepository.removeShoppingCartAndProductByProduct_ProductIdAndShoppingCart_UserName(productName, userName)) {
            throw new ErrorWhenDeletingAnItemFromTheShoppingCart("Ошибка при удаление товара с id " + productName + " в корзине пользователя " + userName);
        }
        return findShoppingCart(userName);
    }

    @Override
    public Optional<ShoppingCartDto> changeQuantityInShoppingCart(String userName, ProductInCat productInCat) {

        final ShoppingCartAndProduct shoppingCartAndProduct = shoppingCartAndProductRepository.findByProduct_ProductId(productInCat.getProductId()).orElse(null);

        shoppingCartAndProduct.setQuantity(productInCat.getQuantity());
        shoppingCartAndProductRepository.save(shoppingCartAndProduct);

        return findShoppingCart(userName);
    }

    private ShoppingCart getShoppingCart(final String userName) {

        final Optional<ShoppingCart> shoppingCartOptional = shoppingCartRepository.findByUserName(userName);

        return shoppingCartOptional.orElse(createShoppingCart(userName));
    }

    private ShoppingCart createShoppingCart(String userName) {
        final ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserName(userName);
        shoppingCart.setShoppingCartState(ShoppingCartState.ACTIVE);
        return shoppingCartRepository.save(shoppingCart);
    }
}
