package ru.yandex.practicum.error;

public class WakeupException extends RuntimeException {
    public WakeupException(String message, Exception e) {
        super(message, e);
    }
}
