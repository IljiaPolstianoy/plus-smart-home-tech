package ru.yandex.practicum.error;

public class ErrorWhenDeletingAnItemFromTheShoppingCart extends RuntimeException {
    public ErrorWhenDeletingAnItemFromTheShoppingCart(String message) {
        super(message);
    }
}
