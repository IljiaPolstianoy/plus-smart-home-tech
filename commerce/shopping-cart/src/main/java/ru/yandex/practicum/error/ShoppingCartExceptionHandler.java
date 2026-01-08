package ru.yandex.practicum.error;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import ru.yandex.practicum.model.error.ApiErrorResponse;
import ru.yandex.practicum.model.error.NotAuthorizedUserException;
import ru.yandex.practicum.validation.ExceptionType;

import java.time.LocalDateTime;

@RestControllerAdvice
@RequiredArgsConstructor
public class ShoppingCartExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            WebRequest request) {

        // Берем первое нарушение
        var violation = ex.getConstraintViolations().iterator().next();
        var attributes = violation.getConstraintDescriptor().getAttributes();

        // Получаем ExceptionType из аннотации
        ExceptionType exceptionType = (ExceptionType) attributes.get("exceptionType");

        // Если exceptionType не найден, используем NOT_AUTHORIZED по умолчанию
        if (exceptionType == null) {
            exceptionType = ExceptionType.NOT_AUTHORIZED;
        }

        // Получаем путь запроса
        String path = getRequestPath(request);

        // Создаем соответствующее исключение
        NotAuthorizedUserException exception = createExceptionByType(
                exceptionType,
                violation.getMessage()
        );

        // Создаем DTO ответа
        ApiErrorResponse errorResponse = buildApiErrorResponse(exception, exceptionType, path);

        // Возвращаем с нужным статусом
        return ResponseEntity.status(exceptionType.getHttpStatus()).body(errorResponse);
    }

    @ExceptionHandler(NotAuthorizedUserException.class)
    public ResponseEntity<ApiErrorResponse> handleNotAuthorizedUserException(
            NotAuthorizedUserException ex,
            WebRequest request) {

        String path = getRequestPath(request);
        ApiErrorResponse errorResponse = buildApiErrorResponse(ex, ExceptionType.NOT_AUTHORIZED, path);

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(errorResponse);
    }

    private NotAuthorizedUserException createExceptionByType(ExceptionType type, String message) {
        if (type == ExceptionType.NOT_AUTHORIZED) {
            return new NotAuthorizedUserException(
                    message,
                    "Пользователь не авторизован",
                    "UNAUTHORIZED"
            );
        }
        return new NotAuthorizedUserException(
                message,
                "Ошибка валидации",
                "BAD_REQUEST"
        );
    }

    private ApiErrorResponse buildApiErrorResponse(
            NotAuthorizedUserException ex,
            ExceptionType exceptionType,
            String path) {

        return ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .httpStatus(exceptionType.getHttpStatus())
                .statusCode(exceptionType.getHttpStatus().value())
                .error(exceptionType.getHttpStatus().getReasonPhrase())
                .message(ex.getMessage())
                .userMessage(ex.getUserMessage())
                .localizedMessage(ex.getLocalizedMessage())
                .path(path)
                .errorCode(exceptionType.name())
                .cause(convertCause(ex.getErrorCause()))
                .build();
    }

    private ApiErrorResponse.CauseInfo convertCause(NotAuthorizedUserException.Cause cause) {
        if (cause == null) return null;

        return ApiErrorResponse.CauseInfo.builder()
                .message(cause.getMessage())
                .localizedMessage(cause.getLocalizedMessage())
                .build();
    }

    private String getRequestPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return "unknown";
    }
}