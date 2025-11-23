package ru.practicum.collector.error;

public class DeserializationException extends RuntimeException {
    public DeserializationException(String message, Exception e) {
        super(message, e);
    }
}
