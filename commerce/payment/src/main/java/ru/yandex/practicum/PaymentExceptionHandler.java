package ru.yandex.practicum;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import ru.yandex.practicum.model.error.NoPaymentFoundException;
import ru.yandex.practicum.model.error.NotEnoughInfoInOrderToCalculateException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class PaymentExceptionHandler {

    /**
     * Обработчик для NoPaymentFoundException
     */
    @ExceptionHandler(NoPaymentFoundException.class)
    public ResponseEntity<NoPaymentFoundException> handleNoPaymentFoundException(
            NoPaymentFoundException ex) {
        log.warn("Payment not found: {}", ex.getMessage());

        NoPaymentFoundException enrichedEx = enrichNoPaymentFoundException(ex);

        HttpStatus status = getHttpStatusForException(enrichedEx);
        return ResponseEntity
                .status(status)
                .body(enrichedEx);
    }

    /**
     * Обработчик для NotEnoughInfoInOrderToCalculateException
     */
    @ExceptionHandler(NotEnoughInfoInOrderToCalculateException.class)
    public ResponseEntity<NotEnoughInfoInOrderToCalculateException> handleNotEnoughInfoException(
            NotEnoughInfoInOrderToCalculateException ex) {
        log.warn("Not enough information for calculation: {}", ex.getMessage());

        NotEnoughInfoInOrderToCalculateException enrichedEx = enrichNotEnoughInfoException(ex);

        HttpStatus status = getHttpStatusForException(enrichedEx);
        return ResponseEntity
                .status(status)
                .body(enrichedEx);
    }

    /**
     * Обогащает NoPaymentFoundException
     */
    private NoPaymentFoundException enrichNoPaymentFoundException(NoPaymentFoundException ex) {
        if (ex.getJsonStackTrace() == null) {
            List<NoPaymentFoundException.StackTraceElement> stackTrace =
                    convertStackTraceForNoPaymentFoundException(ex.getStackTrace());
            ex.setJsonStackTrace(stackTrace);
        }

        if (ex.getErrorCause() == null && ex.getCause() != null) {
            NoPaymentFoundException.Cause cause = new NoPaymentFoundException.Cause();
            cause.setMessage(ex.getCause().getMessage());
            cause.setLocalizedMessage(ex.getCause().getLocalizedMessage());
            cause.setStackTrace(convertStackTraceForNoPaymentFoundException(
                    ex.getCause().getStackTrace()
            ));
            ex.setErrorCause(cause);
        }

        if (ex.getSuppressedCauses() == null && ex.getSuppressed() != null &&
                ex.getSuppressed().length > 0) {
            List<NoPaymentFoundException.Cause> suppressedCauses = Arrays.stream(ex.getSuppressed())
                    .map(suppressed -> {
                        NoPaymentFoundException.Cause cause = new NoPaymentFoundException.Cause();
                        cause.setMessage(suppressed.getMessage());
                        cause.setLocalizedMessage(suppressed.getLocalizedMessage());
                        cause.setStackTrace(convertStackTraceForNoPaymentFoundException(
                                suppressed.getStackTrace()
                        ));
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

        return ex;
    }

    /**
     * Обогащает NotEnoughInfoInOrderToCalculateException
     */
    private NotEnoughInfoInOrderToCalculateException enrichNotEnoughInfoException(
            NotEnoughInfoInOrderToCalculateException ex) {
        if (ex.getJsonStackTrace() == null) {
            List<NotEnoughInfoInOrderToCalculateException.StackTraceElement> stackTrace =
                    convertStackTraceForNotEnoughInfoException(ex.getStackTrace());
            ex.setJsonStackTrace(stackTrace);
        }

        if (ex.getErrorCause() == null && ex.getCause() != null) {
            NotEnoughInfoInOrderToCalculateException.Cause cause =
                    new NotEnoughInfoInOrderToCalculateException.Cause();
            cause.setMessage(ex.getCause().getMessage());
            cause.setLocalizedMessage(ex.getCause().getLocalizedMessage());
            cause.setStackTrace(convertStackTraceForNotEnoughInfoException(
                    ex.getCause().getStackTrace()
            ));
            ex.setErrorCause(cause);
        }

        if (ex.getSuppressedCauses() == null && ex.getSuppressed() != null &&
                ex.getSuppressed().length > 0) {
            List<NotEnoughInfoInOrderToCalculateException.Cause> suppressedCauses = Arrays.stream(ex.getSuppressed())
                    .map(suppressed -> {
                        NotEnoughInfoInOrderToCalculateException.Cause cause =
                                new NotEnoughInfoInOrderToCalculateException.Cause();
                        cause.setMessage(suppressed.getMessage());
                        cause.setLocalizedMessage(suppressed.getLocalizedMessage());
                        cause.setStackTrace(convertStackTraceForNotEnoughInfoException(
                                suppressed.getStackTrace()
                        ));
                        return cause;
                    })
                    .collect(Collectors.toList());
            ex.setSuppressedCauses(suppressedCauses);
        }

        if (ex.getHttpStatus() == null) {
            ex.setHttpStatus(HttpStatus.BAD_REQUEST.name());
        }

        if (ex.getLocalizedMessage() == null) {
            ex.setLocalizedMessage(ex.getLocalizedMessage() != null ?
                    ex.getLocalizedMessage() : ex.getMessage());
        }

        return ex;
    }

    /**
     * Получает HTTP статус из исключения
     */
    private HttpStatus getHttpStatusForException(RuntimeException exception) {
        if (exception instanceof NoPaymentFoundException) {
            NoPaymentFoundException ex = (NoPaymentFoundException) exception;
            if (ex.getHttpStatus() != null) {
                try {
                    return HttpStatus.valueOf(ex.getHttpStatus());
                } catch (IllegalArgumentException e) {
                    log.debug("Invalid HTTP status in exception: {}", ex.getHttpStatus());
                }
            }
            return HttpStatus.NOT_FOUND;
        } else if (exception instanceof NotEnoughInfoInOrderToCalculateException) {
            NotEnoughInfoInOrderToCalculateException ex = (NotEnoughInfoInOrderToCalculateException) exception;
            if (ex.getHttpStatus() != null) {
                try {
                    return HttpStatus.valueOf(ex.getHttpStatus());
                } catch (IllegalArgumentException e) {
                    log.debug("Invalid HTTP status in exception: {}", ex.getHttpStatus());
                }
            }
            return HttpStatus.BAD_REQUEST;
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * Конвертирует StackTraceElement[] для NoPaymentFoundException
     */
    private List<NoPaymentFoundException.StackTraceElement> convertStackTraceForNoPaymentFoundException(
            StackTraceElement[] stackTrace) {
        if (stackTrace == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(stackTrace)
                .map(element -> {
                    NoPaymentFoundException.StackTraceElement jsonElement =
                            new NoPaymentFoundException.StackTraceElement();
                    jsonElement.setMethodName(element.getMethodName());
                    jsonElement.setFileName(element.getFileName());
                    jsonElement.setLineNumber(element.getLineNumber());
                    jsonElement.setClassName(element.getClassName());
                    jsonElement.setNativeMethod(element.isNativeMethod());
                    jsonElement.setClassLoaderName(element.getClassLoaderName());
                    jsonElement.setModuleName(element.getModuleName());
                    jsonElement.setModuleVersion(element.getModuleVersion());
                    return jsonElement;
                })
                .collect(Collectors.toList());
    }

    /**
     * Конвертирует StackTraceElement[] для NotEnoughInfoInOrderToCalculateException
     */
    private List<NotEnoughInfoInOrderToCalculateException.StackTraceElement> convertStackTraceForNotEnoughInfoException(
            StackTraceElement[] stackTrace) {
        if (stackTrace == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(stackTrace)
                .map(element -> {
                    NotEnoughInfoInOrderToCalculateException.StackTraceElement jsonElement =
                            new NotEnoughInfoInOrderToCalculateException.StackTraceElement();
                    jsonElement.setMethodName(element.getMethodName());
                    jsonElement.setFileName(element.getFileName());
                    jsonElement.setLineNumber(element.getLineNumber());
                    jsonElement.setClassName(element.getClassName());
                    jsonElement.setNativeMethod(element.isNativeMethod());
                    jsonElement.setClassLoaderName(element.getClassLoaderName());
                    jsonElement.setModuleName(element.getModuleName());
                    jsonElement.setModuleVersion(element.getModuleVersion());
                    return jsonElement;
                })
                .collect(Collectors.toList());
    }

    /**
     * Обработчик для валидации параметров запроса
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<NoPaymentFoundException> handleValidationException(
            MethodArgumentNotValidException ex) {
        log.warn("Validation error in payment: {}", ex.getMessage());

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        NoPaymentFoundException exception = new NoPaymentFoundException(
                "Validation failed: " + errorMessage,
                "Ошибка валидации параметров платежа",
                HttpStatus.BAD_REQUEST
        );

        List<NoPaymentFoundException.StackTraceElement> stackTrace = convertStackTraceForNoPaymentFoundException(
                ex.getStackTrace()
        );
        exception.setJsonStackTrace(stackTrace);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception);
    }

    /**
     * Обработчик для ConstraintViolationException
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<NoPaymentFoundException> handleConstraintViolationException(
            ConstraintViolationException ex) {
        log.warn("Constraint violation in payment: {}", ex.getMessage());

        String errorMessage = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        NoPaymentFoundException exception = new NoPaymentFoundException(
                "Constraint violation: " + errorMessage,
                "Нарушение ограничений данных платежа",
                HttpStatus.BAD_REQUEST
        );

        List<NoPaymentFoundException.StackTraceElement> stackTrace = convertStackTraceForNoPaymentFoundException(
                ex.getStackTrace()
        );
        exception.setJsonStackTrace(stackTrace);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(exception);
    }

    /**
     * Обработчик для 404 (не найден обработчик)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<NoPaymentFoundException> handleNoHandlerFoundException(
            NoHandlerFoundException ex) {
        log.warn("Payment endpoint not found: {} {}", ex.getHttpMethod(), ex.getRequestURL());

        NoPaymentFoundException exception = new NoPaymentFoundException(
                String.format("Endpoint %s %s not found", ex.getHttpMethod(), ex.getRequestURL()),
                "Запрашиваемый ресурс платежной системы не найден",
                HttpStatus.NOT_FOUND
        );

        List<NoPaymentFoundException.StackTraceElement> stackTrace = convertStackTraceForNoPaymentFoundException(
                ex.getStackTrace()
        );
        exception.setJsonStackTrace(stackTrace);

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(exception);
    }

    /**
     * Обработчик для всех остальных исключений
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<NoPaymentFoundException> handleAllExceptions(Exception ex) {
        log.error("Internal server error in payment: {}", ex.getMessage(), ex);

        NoPaymentFoundException exception = new NoPaymentFoundException(
                "Internal server error: " + ex.getMessage(),
                "Внутренняя ошибка сервера при обработке платежа",
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex
        );

        List<NoPaymentFoundException.StackTraceElement> stackTrace = convertStackTraceForNoPaymentFoundException(
                ex.getStackTrace()
        );
        exception.setJsonStackTrace(stackTrace);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(exception);
    }
}