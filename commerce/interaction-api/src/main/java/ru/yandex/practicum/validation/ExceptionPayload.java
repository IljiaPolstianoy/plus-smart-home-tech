package ru.yandex.practicum.validation;

import jakarta.validation.Payload;
import jakarta.validation.ValidationException;
import ru.yandex.practicum.model.error.NotAuthorizedUserException;

public interface ExceptionPayload extends Payload {
    // Метод, который возвращает класс исключения
    Class<? extends RuntimeException> exceptionType();

    // Конкретные реализации для каждого типа исключения
    class NotAuthorizedPayload implements ExceptionPayload {
        @Override
        public Class<? extends RuntimeException> exceptionType() {
            return NotAuthorizedUserException.class;
        }
    }

    class ValidationPayload implements ExceptionPayload {
        @Override
        public Class<? extends RuntimeException> exceptionType() {
            return ValidationException.class;
        }
    }
}