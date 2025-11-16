package ru.practicum.collector.error;

public class SerializationException extends RuntimeException {
    public SerializationException(String message, Exception e) {
        super(message, e);
    }
}
