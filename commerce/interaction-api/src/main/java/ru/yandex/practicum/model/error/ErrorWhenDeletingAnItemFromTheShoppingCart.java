package ru.yandex.practicum.model.error;

public class ErrorWhenDeletingAnItemFromTheShoppingCart extends RuntimeException {
    public ErrorWhenDeletingAnItemFromTheShoppingCart(String message) {
        super(message);
    }
}
