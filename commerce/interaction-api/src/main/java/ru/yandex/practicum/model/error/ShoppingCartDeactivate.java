package ru.yandex.practicum.model.error;

public class ShoppingCartDeactivate extends RuntimeException{
    public ShoppingCartDeactivate(String message){
        super(message);
    }
}
