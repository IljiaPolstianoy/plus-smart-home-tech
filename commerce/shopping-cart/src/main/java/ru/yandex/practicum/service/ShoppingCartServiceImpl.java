package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.feign.WarehouseFeignClient;
import ru.yandex.practicum.model.error.ErrorWhenDeletingAnItemFromTheShoppingCart;
import ru.yandex.practicum.model.error.NoProductsInShoppingCartException;
import ru.yandex.practicum.model.error.ShoppingCartDeactivate;
import ru.yandex.practicum.model.product.ProductDto;
import ru.yandex.practicum.model.quantity.ChangeProductQuantityRequest;
import ru.yandex.practicum.model.shopping.*;
import ru.yandex.practicum.storage.ProductRepository;
import ru.yandex.practicum.storage.ShoppingCartAndProductRepository;
import ru.yandex.practicum.storage.ShoppingCartRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ShoppingCartServiceImpl implements ShoppingCartService {

    private final ShoppingCartRepository shoppingCartRepository;
    private final ShoppingCartAndProductRepository shoppingCartAndProductRepository;
    private final ProductRepository productRepository;
    private final WarehouseFeignClient warehouseFeignClient;

    @Override
    public Optional<ShoppingCartDto> findShoppingCart(final String userName) {

        final ShoppingCart shoppingCart = getShoppingCart(userName);

        if (shoppingCart.getShoppingCartState().equals(ShoppingCartState.DELIVERED)) {
            throw new ShoppingCartDeactivate("Корзина с id " + shoppingCart.getShoppingCartId() + " неактивна");
        }

        final List<ShoppingCartAndProduct> shoppingCartAndProducts = shoppingCartAndProductRepository
                .findByShoppingCartId(shoppingCart.getShoppingCartId());

        final List<ProductInCat> products = new ArrayList<>();

        for (ShoppingCartAndProduct shoppingCartAndProduct : shoppingCartAndProducts) {
            final ProductInCat product = new ProductInCat();
            product.setProductId(shoppingCartAndProduct.getProduct().getProductId());
            product.setQuantity(product.getQuantity());
            products.add(product);
        }
        return Optional.of(ShoppingCartDto.builder()
                .shoppingCartId(shoppingCart.getShoppingCartId())
                .products(products)
                .build());
    }

    @Override
    public Optional<ShoppingCartDto> addProductsInShoppingCart(
            final String userName,
            final List<ChangeProductQuantityRequest> changeProductQuantityRequests
    ) {

        final ShoppingCart shoppingCart = getShoppingCart(userName);

        if (shoppingCart.getShoppingCartState().equals(ShoppingCartState.DELIVERED)) {
            throw new ShoppingCartDeactivate("Корзина с id " + shoppingCart.getShoppingCartId() + " неактивна");
        }

        final ShoppingCartDto shoppingCartDtoForCheckQuantity = findShoppingCart(userName).get();
        final List<ProductInCat> productsInShoppingCart = new ArrayList<>();
        for (ChangeProductQuantityRequest changeProductQuantityRequest : changeProductQuantityRequests) {
            productsInShoppingCart.add(
                    new ProductInCat(
                            changeProductQuantityRequest.getProductId(),
                            changeProductQuantityRequest.getQuantity()
                    )
            );
        }
        shoppingCartDtoForCheckQuantity.addProduct(productsInShoppingCart);
        warehouseFeignClient.checkQuantity(shoppingCartDtoForCheckQuantity);

        for (ChangeProductQuantityRequest changeProductQuantityRequest : changeProductQuantityRequests) {
            final ShoppingCartAndProduct shoppingCartAndProduct = ShoppingCartAndProduct.builder()
                    .product(productRepository.findByProductId(changeProductQuantityRequest.getProductId()).get())
                    .shoppingCart(shoppingCart)
                    .quantity(changeProductQuantityRequest.getQuantity())
                    .build();
            shoppingCartAndProductRepository.save(shoppingCartAndProduct);
        }


        return findShoppingCart(userName);
    }

    @Override
    public boolean removeShoppingCart(final String userName) {

        final ShoppingCart shoppingCart = getShoppingCart(userName);

        shoppingCart.setShoppingCartState(ShoppingCartState.DELIVERED);
        shoppingCartRepository.save(shoppingCart);

        return true;
    }

    @Override
    public Optional<ShoppingCartDto> removeProductsInShoppingCart(
            final String userName,
            final List<String> productName
    ) {
        checkProductsInCat(userName, productName);
        if (!shoppingCartAndProductRepository.removeAllFromCart(productName, userName)) {
            throw new ErrorWhenDeletingAnItemFromTheShoppingCart(
                    "Ошибка при удаление товара с id "
                            + productName
                            + " в корзине пользователя "
                            + userName
            );
        }
        return findShoppingCart(userName);
    }

    @Override
    public Optional<ShoppingCartDto> changeQuantityInShoppingCart(
            final String userName,
            final ChangeProductQuantityRequest changeProductQuantityRequest
    ) {
        final ShoppingCartAndProduct shoppingCartAndProduct = shoppingCartAndProductRepository
                .findByShoppingCartUserNameAndProductId(
                        userName,
                        changeProductQuantityRequest.getProductId()
                ).orElseThrow(() -> new NoProductsInShoppingCartException(
                        "Корзина пуста",
                        "В вашей корзине нет товаров с id " + changeProductQuantityRequest.getProductId() + ".",
                        "400 BAD_REQUEST"
                ));

        shoppingCartAndProduct.setQuantity(changeProductQuantityRequest.getQuantity());
        shoppingCartAndProductRepository.save(shoppingCartAndProduct);

        return findShoppingCart(userName);
    }

    private ShoppingCart getShoppingCart(final String userName) {

        final Optional<ShoppingCart> shoppingCartOptional = shoppingCartRepository.findByUserName(userName);

        return shoppingCartOptional.orElse(createShoppingCart(userName));
    }

    private ShoppingCart createShoppingCart(final String userName) {
        final ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserName(userName);
        shoppingCart.setShoppingCartState(ShoppingCartState.ACTIVE);
        return shoppingCartRepository.save(shoppingCart);
    }

    private boolean checkProductsInCat(
            final String userName,
            final List<String> productName
    ) {

        final List<ShoppingCartAndProduct> shoppingCartAndProducts = shoppingCartAndProductRepository
                .findByShoppingCartId(userName);
        final List<String> products = shoppingCartAndProducts.stream()
                .map(ShoppingCartAndProduct::getProduct)
                .map(ProductDto::getProductId)
                .toList();
        if (!products.containsAll(productName)) {
            // Находим недостающие товары
            final List<String> missingProducts = new ArrayList<>(productName);
            missingProducts.removeAll(products);

            throw new NoProductsInShoppingCartException(
                    "Следующие товары не найдены: " + missingProducts,
                    "Товары не найдены: " + String.join(", ", missingProducts),
                    "BAD_REQUEST"
            );
        }
        return true;
    }
}
