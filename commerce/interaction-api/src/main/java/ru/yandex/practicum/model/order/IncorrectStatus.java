package ru.yandex.practicum.model.order;

public class IncorrectStatus extends RuntimeException {
    public IncorrectStatus(String message) {
        super(message);
    }
}
