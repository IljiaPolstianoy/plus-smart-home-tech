package ru.yandex.practicum.error;

public class DeserializationException extends RuntimeException {
    public DeserializationException(String message, Exception e) {
        super(message, e);
    }
}
