package ru.yandex.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import ru.yandex.practicum.model.error.ApiErrorResponse;
import ru.yandex.practicum.model.error.ProductNotFoundException;

@RestControllerAdvice
@Slf4j
public class ShoppingStoreExceptionHandler {

    /**
     * Обрабатывает ProductNotFoundException и возвращает 404 Not Found
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleProductNotFoundException(
            final ProductNotFoundException ex,
            final WebRequest request) {

        log.error("Product not found: {}", ex.getMessage(), ex);

        // Получаем путь запроса
        String path = getRequestPath(request);

        // Создаем DTO ответа
        ApiErrorResponse errorResponse = ApiErrorResponse.fromProductNotFoundException(ex, path);

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorResponse);
    }

    private String getRequestPath(final WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return "unknown";
    }
}
