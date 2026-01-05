package ru.yandex.practicum.validation;

import jakarta.validation.ValidationException;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import ru.yandex.practicum.model.error.NotAuthorizedUserException;

@Getter
public enum ExceptionType {
    NOT_AUTHORIZED(NotAuthorizedUserException.class, HttpStatus.UNAUTHORIZED, "Пользователь не авторизован"),
    VALIDATION(ValidationException.class, HttpStatus.BAD_REQUEST, "Ошибка валидации");

    private final Class<? extends RuntimeException> exceptionClass;
    private final HttpStatus httpStatus;
    private final String userMessage;

    ExceptionType(Class<? extends RuntimeException> exceptionClass,
                  HttpStatus httpStatus, String userMessage) {
        this.exceptionClass = exceptionClass;
        this.httpStatus = httpStatus;
        this.userMessage = userMessage;
    }

}