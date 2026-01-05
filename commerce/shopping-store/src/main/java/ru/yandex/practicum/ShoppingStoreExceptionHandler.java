package ru.yandex.practicum;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.model.error.ProductNotFoundException;

@RestControllerAdvice
public class ShoppingStoreExceptionHandler {

    /**
     * Обрабатывает ProductNotFoundException и возвращает 404 Not Found
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Object> handleProductNotFoundException(
            ProductNotFoundException ex) {

        // Формируем тело ответа (можно использовать DTO или Map)
        var errorResponse = new java.util.HashMap<String, Object>();
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("userMessage", ex.getUserMessage());
        errorResponse.put("httpStatus", ex.getHttpStatus());
        errorResponse.put("localizedMessage", ex.getLocalizedMessage());

        if (ex.getErrorCause() != null) {
            errorResponse.put("cause", ex.getErrorCause());
        }
        if (ex.getSuppressedCauses() != null && !ex.getSuppressedCauses().isEmpty()) {
            errorResponse.put("suppressed", ex.getSuppressedCauses());
        }

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }
}
