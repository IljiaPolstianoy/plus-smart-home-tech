package ru.yandex.practicum.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Исключение, указывающее, что количество товара в корзине превышает доступный остаток на складе.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProductInShoppingCartLowQuantityInWarehouseException extends RuntimeException {

    // Вложенная структура для элемента stackTrace
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class StackTraceElement {
        private String classLoaderName;
        private String moduleName;
        private String moduleVersion;
        private String methodName;
        private String fileName;
        private Integer lineNumber;
        private String className;
        private Boolean nativeMethod;
    }

    // Основное поле cause (может быть null)
    private Cause cause;

    // Список элементов stackTrace
    private List<StackTraceElement> stackTrace;

    // HTTP-статус (например, "400 BAD_REQUEST")
    private String httpStatus;

    // Пользовательское сообщение (для клиента)
    private String userMessage;

    // Стандартное сообщение исключения
    private String message;

    // Локализованное сообщение
    private String localizedMessage;

    // Подавленные исключения (редко используются, но по спецификации)
    private List<Cause> suppressed;

    // Вложенная структура Cause (повторение основной структуры)
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Cause {
        private List<StackTraceElement> stackTrace;
        private String message;
        private String localizedMessage;
    }

    // Конструкторы

    public ProductInShoppingCartLowQuantityInWarehouseException(String message) {
        super(message);
        this.message = message;
    }

    public ProductInShoppingCartLowQuantityInWarehouseException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.cause = new Cause();
        this.cause.setMessage(cause.getMessage());
        this.cause.setLocalizedMessage(cause.getLocalizedMessage());
    }

    public ProductInShoppingCartLowQuantityInWarehouseException(
            String message,
            String userMessage,
            HttpStatus httpStatus) {
        super(message);
        this.message = message;
        this.userMessage = userMessage;
        this.httpStatus = httpStatus.toString();
    }

    public ProductInShoppingCartLowQuantityInWarehouseException(
            String message,
            String userMessage,
            String httpStatus) {
        super(message);
        this.message = message;
        this.userMessage = userMessage;
        this.httpStatus = httpStatus;
    }
}

