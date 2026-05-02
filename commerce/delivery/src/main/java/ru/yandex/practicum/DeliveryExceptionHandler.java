package ru.yandex.practicum;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import ru.yandex.practicum.model.error.NoDeliveryFoundException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class DeliveryExceptionHandler {

    /**
     * Обработчик для NoDeliveryFoundException
     */
    @ExceptionHandler(NoDeliveryFoundException.class)
    public ResponseEntity<NoDeliveryFoundException> handleNoDeliveryFoundException(
            NoDeliveryFoundException ex) {
        log.warn("Delivery not found: {}", ex.getMessage());

        if (ex.getJsonStackTrace() == null) {
            ex.setJsonStackTrace(convertStackTrace(ex.getStackTrace()));
        }

        if (ex.getCause() != null && ex.getErrorCause() == null) {
            Throwable cause = ex.getCause();
            NoDeliveryFoundException.Cause errorCause = new NoDeliveryFoundException.Cause();
            errorCause.setMessage(cause.getMessage());
            errorCause.setLocalizedMessage(cause.getLocalizedMessage());
            errorCause.setStackTrace(convertStackTrace(cause.getStackTrace()));
            ex.setErrorCause(errorCause);
        }

        if (ex.getSuppressed() != null && ex.getSuppressed().length > 0 &&
                ex.getSuppressedCauses() == null) {
            List<NoDeliveryFoundException.Cause> suppressedCauses = Arrays.stream(ex.getSuppressed())
                    .map(suppressed -> {
                        NoDeliveryFoundException.Cause cause = new NoDeliveryFoundException.Cause();
                        cause.setMessage(suppressed.getMessage());
                        cause.setLocalizedMessage(suppressed.getLocalizedMessage());
                        cause.setStackTrace(convertStackTrace(suppressed.getStackTrace()));
                        return cause;
                    })
                    .collect(Collectors.toList());
            ex.setSuppressedCauses(suppressedCauses);
        }

        if (ex.getHttpStatus() == null) {
            ex.setHttpStatus(HttpStatus.NOT_FOUND.name());
        }

        if (ex.getLocalizedMessage() == null) {
            ex.setLocalizedMessage(ex.getLocalizedMessage() != null ?
                    ex.getLocalizedMessage() : ex.getMessage());
        }

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ex);
    }

    /**
     * Обработчик для валидации параметров запроса
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<NoDeliveryFoundException> handleValidationException(
            MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        NoDeliveryFoundException exception = new NoDeliveryFoundException(
                "Validation failed: " + errorMessage,
                "Ошибка валидации параметров запроса",
                HttpStatus.BAD_REQUEST
        );
        exception.setJsonStackTrace(convertStackTrace(ex.getStackTrace()));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception);
    }

    /**
     * Обработчик для ConstraintViolationException (валидация на уровне метода)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<NoDeliveryFoundException> handleConstraintViolationException(
            ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());

        String errorMessage = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        NoDeliveryFoundException exception = new NoDeliveryFoundException(
                "Constraint violation: " + errorMessage,
                "Нарушение ограничений данных",
                HttpStatus.BAD_REQUEST
        );
        exception.setJsonStackTrace(convertStackTrace(ex.getStackTrace()));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception);
    }

    /**
     * Обработчик для 404 (не найден обработчик)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<NoDeliveryFoundException> handleNoHandlerFoundException(
            NoHandlerFoundException ex) {
        log.warn("Endpoint not found: {} {}", ex.getHttpMethod(), ex.getRequestURL());

        NoDeliveryFoundException exception = new NoDeliveryFoundException(
                String.format("Endpoint %s %s not found", ex.getHttpMethod(), ex.getRequestURL()),
                "Запрашиваемый ресурс не найден",
                HttpStatus.NOT_FOUND
        );
        exception.setJsonStackTrace(convertStackTrace(ex.getStackTrace()));

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(exception);
    }

    /**
     * Обработчик для всех остальных исключений
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<NoDeliveryFoundException> handleAllExceptions(Exception ex) {
        log.error("Internal server error: {}", ex.getMessage(), ex);

        NoDeliveryFoundException exception = new NoDeliveryFoundException(
                "Internal server error: " + ex.getMessage(),
                "Внутренняя ошибка сервера",
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex
        );
        exception.setJsonStackTrace(convertStackTrace(ex.getStackTrace()));

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(exception);
    }

    /**
     * Конвертирует StackTraceElement[] в список StackTraceElement для JSON
     */
    private List<NoDeliveryFoundException.StackTraceElement> convertStackTrace(StackTraceElement[] stackTrace) {
        if (stackTrace == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(stackTrace)
                .map(element -> {
                    NoDeliveryFoundException.StackTraceElement jsonElement =
                            new NoDeliveryFoundException.StackTraceElement();
                    jsonElement.setMethodName(element.getMethodName());
                    jsonElement.setFileName(element.getFileName());
                    jsonElement.setLineNumber(element.getLineNumber());
                    jsonElement.setClassName(element.getClassName());
                    jsonElement.setNativeMethod(element.isNativeMethod());
                    // Эти поля могут быть null в некоторых случаях
                    jsonElement.setClassLoaderName(element.getClassLoaderName());
                    jsonElement.setModuleName(element.getModuleName());
                    jsonElement.setModuleVersion(element.getModuleVersion());
                    return jsonElement;
                })
                .collect(Collectors.toList());
    }
}