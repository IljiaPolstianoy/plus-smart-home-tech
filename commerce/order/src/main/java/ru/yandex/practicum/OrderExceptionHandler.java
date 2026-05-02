package ru.yandex.practicum;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import ru.yandex.practicum.model.error.NoOrderFoundException;
import ru.yandex.practicum.model.error.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.model.error.NotAuthorizedUserException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class OrderExceptionHandler {

    /**
     * Обработчик для NoOrderFoundException
     */
    @ExceptionHandler(NoOrderFoundException.class)
    public ResponseEntity<NoOrderFoundException> handleNoOrderFoundException(
            NoOrderFoundException ex) {
        log.warn("Order not found: {}", ex.getMessage());

        enrichException(ex);

        HttpStatus status = getHttpStatusFromException(ex);
        return ResponseEntity
                .status(status)
                .body(ex);
    }

    /**
     * Обработчик для NoSpecifiedProductInWarehouseException
     */
    @ExceptionHandler(NoSpecifiedProductInWarehouseException.class)
    public ResponseEntity<NoSpecifiedProductInWarehouseException> handleNoProductInWarehouseException(
            NoSpecifiedProductInWarehouseException ex) {
        log.warn("Product not found in warehouse: {}", ex.getMessage());

        enrichException(ex);

        HttpStatus status = getHttpStatusFromException(ex);
        return ResponseEntity
                .status(status)
                .body(ex);
    }

    /**
     * Обработчик для NotAuthorizedUserException
     */
    @ExceptionHandler(NotAuthorizedUserException.class)
    public ResponseEntity<NotAuthorizedUserException> handleNotAuthorizedUserException(
            NotAuthorizedUserException ex) {
        log.warn("User not authorized: {}", ex.getMessage());

        enrichException(ex);

        HttpStatus status = getHttpStatusFromException(ex);
        return ResponseEntity
                .status(status)
                .body(ex);
    }

    /**
     * Обогащает исключение дополнительными данными
     */
    private <T extends RuntimeException> void enrichException(T exception) {
        try {
            Class<?> clazz = exception.getClass();

            Field stackTraceField = clazz.getDeclaredField("jsonStackTrace");
            stackTraceField.setAccessible(true);
            if (stackTraceField.get(exception) == null) {
                Class<?> stackTraceElementClass = getStackTraceElementClass(clazz);
                List<?> stackTrace = convertStackTrace(exception.getStackTrace(), stackTraceElementClass);
                stackTraceField.set(exception, stackTrace);
            }

            Field causeField = clazz.getDeclaredField("errorCause");
            causeField.setAccessible(true);
            if (causeField.get(exception) == null && exception.getCause() != null) {
                Class<?> causeClass = getCauseClass(clazz);
                Object cause = causeClass.getDeclaredConstructor().newInstance();

                Field causeMessageField = causeClass.getDeclaredField("message");
                causeMessageField.setAccessible(true);
                causeMessageField.set(cause, exception.getCause().getMessage());

                Field causeLocalizedField = causeClass.getDeclaredField("localizedMessage");
                causeLocalizedField.setAccessible(true);
                causeLocalizedField.set(cause, exception.getCause().getLocalizedMessage());

                Field causeStackTraceField = causeClass.getDeclaredField("stackTrace");
                causeStackTraceField.setAccessible(true);
                Class<?> causeStackTraceElementClass = getStackTraceElementClass(clazz);
                List<?> causeStackTrace = convertStackTrace(
                        exception.getCause().getStackTrace(),
                        causeStackTraceElementClass
                );
                causeStackTraceField.set(cause, causeStackTrace);

                causeField.set(exception, cause);
            }

            Field suppressedField = clazz.getDeclaredField("suppressedCauses");
            suppressedField.setAccessible(true);
            if (suppressedField.get(exception) == null && exception.getSuppressed() != null &&
                    exception.getSuppressed().length > 0) {
                Class<?> causeClass = getCauseClass(clazz);
                Class<?> stackTraceElementClass = getStackTraceElementClass(clazz);

                List<Object> suppressedCauses = Arrays.stream(exception.getSuppressed())
                        .map(suppressed -> {
                            try {
                                Object cause = causeClass.getDeclaredConstructor().newInstance();

                                Field messageField = causeClass.getDeclaredField("message");
                                messageField.setAccessible(true);
                                messageField.set(cause, suppressed.getMessage());

                                Field localizedField = causeClass.getDeclaredField("localizedMessage");
                                localizedField.setAccessible(true);
                                localizedField.set(cause, suppressed.getLocalizedMessage());

                                Field stackTraceField2 = causeClass.getDeclaredField("stackTrace");
                                stackTraceField2.setAccessible(true);
                                List<?> suppressedStackTrace = convertStackTrace(
                                        suppressed.getStackTrace(),
                                        stackTraceElementClass
                                );
                                stackTraceField2.set(cause, suppressedStackTrace);

                                return cause;
                            } catch (Exception e) {
                                log.error("Error creating suppressed cause", e);
                                return null;
                            }
                        })
                        .filter(cause -> cause != null)
                        .collect(Collectors.toList());
                suppressedField.set(exception, suppressedCauses);
            }

            Field httpStatusField = clazz.getDeclaredField("httpStatus");
            httpStatusField.setAccessible(true);
            if (httpStatusField.get(exception) == null) {
                httpStatusField.set(exception, HttpStatus.NOT_FOUND.name());
            }

            Field localizedField = clazz.getDeclaredField("localizedMessage");
            localizedField.setAccessible(true);
            if (localizedField.get(exception) == null) {
                localizedField.set(exception, exception.getLocalizedMessage() != null ?
                        exception.getLocalizedMessage() : exception.getMessage());
            }

        } catch (Exception e) {
            log.error("Error enriching exception", e);
        }
    }

    /**
     * Получает класс Cause для исключения
     */
    private Class<?> getCauseClass(Class<?> exceptionClass) throws Exception {
        Class<?>[] nestedClasses = exceptionClass.getDeclaredClasses();
        for (Class<?> nestedClass : nestedClasses) {
            if (nestedClass.getSimpleName().equals("Cause")) {
                return nestedClass;
            }
        }
        throw new IllegalStateException("Cause class not found in " + exceptionClass.getName());
    }

    /**
     * Получает класс StackTraceElement для исключения
     */
    private Class<?> getStackTraceElementClass(Class<?> exceptionClass) throws Exception {
        Class<?>[] nestedClasses = exceptionClass.getDeclaredClasses();
        for (Class<?> nestedClass : nestedClasses) {
            if (nestedClass.getSimpleName().equals("StackTraceElement")) {
                return nestedClass;
            }
        }
        throw new IllegalStateException("StackTraceElement class not found in " + exceptionClass.getName());
    }

    /**
     * Получает HTTP статус из исключения
     */
    private HttpStatus getHttpStatusFromException(RuntimeException exception) {
        try {
            Field httpStatusField = exception.getClass().getDeclaredField("httpStatus");
            httpStatusField.setAccessible(true);
            String statusStr = (String) httpStatusField.get(exception);

            if (statusStr != null) {
                return HttpStatus.valueOf(statusStr);
            }
        } catch (Exception e) {
            log.debug("Could not get httpStatus from exception", e);
        }

        if (exception instanceof NoOrderFoundException) {
            return HttpStatus.NOT_FOUND;
        } else if (exception instanceof NoSpecifiedProductInWarehouseException) {
            return HttpStatus.NOT_FOUND;
        } else if (exception instanceof NotAuthorizedUserException) {
            return HttpStatus.UNAUTHORIZED;
        }

        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * Обработчик для валидации параметров запроса
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<NoOrderFoundException> handleValidationException(
            MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        NoOrderFoundException exception = new NoOrderFoundException(
                "Validation failed: " + errorMessage,
                "Ошибка валидации параметров заказа",
                HttpStatus.BAD_REQUEST
        );

        List<NoOrderFoundException.StackTraceElement> stackTrace = convertStackTraceForNoOrderFoundException(
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
    public ResponseEntity<NoOrderFoundException> handleConstraintViolationException(
            ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());

        String errorMessage = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining(", "));

        NoOrderFoundException exception = new NoOrderFoundException(
                "Constraint violation: " + errorMessage,
                "Нарушение ограничений данных заказа",
                HttpStatus.BAD_REQUEST
        );

        List<NoOrderFoundException.StackTraceElement> stackTrace = convertStackTraceForNoOrderFoundException(
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
    public ResponseEntity<NoOrderFoundException> handleNoHandlerFoundException(
            NoHandlerFoundException ex) {
        log.warn("Endpoint not found: {} {}", ex.getHttpMethod(), ex.getRequestURL());

        NoOrderFoundException exception = new NoOrderFoundException(
                String.format("Endpoint %s %s not found", ex.getHttpMethod(), ex.getRequestURL()),
                "Запрашиваемый ресурс не найден",
                HttpStatus.NOT_FOUND
        );

        List<NoOrderFoundException.StackTraceElement> stackTrace = convertStackTraceForNoOrderFoundException(
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
    public ResponseEntity<NoOrderFoundException> handleAllExceptions(Exception ex) {
        log.error("Internal server error: {}", ex.getMessage(), ex);

        NoOrderFoundException exception = new NoOrderFoundException(
                "Internal server error: " + ex.getMessage(),
                "Внутренняя ошибка сервера при обработке заказа",
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex
        );

        List<NoOrderFoundException.StackTraceElement> stackTrace = convertStackTraceForNoOrderFoundException(
                ex.getStackTrace()
        );
        exception.setJsonStackTrace(stackTrace);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(exception);
    }

    /**
     * Конвертирует StackTraceElement[] в список StackTraceElement для NoOrderFoundException
     */
    private List<NoOrderFoundException.StackTraceElement> convertStackTraceForNoOrderFoundException(
            StackTraceElement[] stackTrace) {
        if (stackTrace == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(stackTrace)
                .map(element -> {
                    NoOrderFoundException.StackTraceElement jsonElement =
                            new NoOrderFoundException.StackTraceElement();
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
     * Универсальный метод конвертации с рефлексией
     */
    private List<?> convertStackTrace(StackTraceElement[] stackTrace, Class<?> stackTraceElementClass) {
        if (stackTrace == null) {
            return Collections.emptyList();
        }

        try {
            return Arrays.stream(stackTrace)
                    .map(element -> createStackTraceElement(element, stackTraceElementClass))
                    .filter(element -> element != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error converting stack trace", e);
            return Collections.emptyList();
        }
    }

    /**
     * Создает объект StackTraceElement через рефлексию
     */
    private Object createStackTraceElement(StackTraceElement element, Class<?> stackTraceElementClass) {
        try {
            // Получаем конструктор без аргументов
            Constructor<?> constructor = stackTraceElementClass.getDeclaredConstructor();
            Object jsonElement = constructor.newInstance();

            // Устанавливаем поля
            setFieldValue(jsonElement, stackTraceElementClass, "methodName", element.getMethodName());
            setFieldValue(jsonElement, stackTraceElementClass, "fileName", element.getFileName());
            setFieldValue(jsonElement, stackTraceElementClass, "lineNumber", element.getLineNumber());
            setFieldValue(jsonElement, stackTraceElementClass, "className", element.getClassName());
            setFieldValue(jsonElement, stackTraceElementClass, "nativeMethod", element.isNativeMethod());
            setFieldValue(jsonElement, stackTraceElementClass, "classLoaderName", element.getClassLoaderName());
            setFieldValue(jsonElement, stackTraceElementClass, "moduleName", element.getModuleName());
            setFieldValue(jsonElement, stackTraceElementClass, "moduleVersion", element.getModuleVersion());

            return jsonElement;
        } catch (Exception e) {
            log.error("Error creating StackTraceElement", e);
            return null;
        }
    }

    /**
     * Устанавливает значение поля через рефлексию
     */
    private void setFieldValue(Object obj, Class<?> clazz, String fieldName, Object value) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            // Игнорируем, если поле не найдено
            log.debug("Field {} not found in class {}", fieldName, clazz.getName());
        }
    }
}