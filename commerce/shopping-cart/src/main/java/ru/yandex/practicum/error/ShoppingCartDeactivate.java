package ru.yandex.practicum.error;

public class ShoppingCartDeactivate extends RuntimeException{
    public ShoppingCartDeactivate(String message){
        super(message);
    }
}
