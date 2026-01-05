package ru.yandex.practicum.error;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.model.error.NotAuthorizedUserException;
import ru.yandex.practicum.validation.ExceptionType;

import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class ShoppingCartExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public ResponseEntity<Object> handleConstraintViolation(
            ConstraintViolationException ex) {

        // Берем первое нарушение
        var violation = ex.getConstraintViolations().iterator().next();
        Map<String, Object> attributes = violation.getConstraintDescriptor().getAttributes();

        // Получаем ExceptionType из аннотации
        ExceptionType exceptionType = (ExceptionType) attributes.get("exceptionType");

        // Если exceptionType не найден, используем NOT_AUTHORIZED по умолчанию
        if (exceptionType == null) {
            exceptionType = ExceptionType.NOT_AUTHORIZED;
        }

        // Создаем соответствующее исключение
        RuntimeException exception = createExceptionByType(
                exceptionType,
                violation.getMessage()
        );

        // Возвращаем с нужным статусом
        return ResponseEntity.status(exceptionType.getHttpStatus()).body(exception);
    }

    private RuntimeException createExceptionByType(ExceptionType type, String message) {
        if (Objects.requireNonNull(type) == ExceptionType.NOT_AUTHORIZED) {
            return new NotAuthorizedUserException(
                    message,
                    "Пользователь не авторизован",
                    "UNAUTHORIZED"
            );
        }
        return new NotAuthorizedUserException(
                message,
                "Ошибка",
                "BAD_REQUEST"
        );
    }
}
