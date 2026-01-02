package ru.yandex.practicum.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.model.error.ProductInShoppingCartLowQuantityInWarehouseException;

@RestControllerAdvice
public class ShoppingCartExceptionHandler {

    @ExceptionHandler(ProductInShoppingCartLowQuantityInWarehouseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProductInShoppingCartLowQuantityInWarehouseException handleException(ProductInShoppingCartLowQuantityInWarehouseException ex) {
        return ex;
    }
}
