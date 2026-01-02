package ru.yandex.practicum;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.model.error.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.model.error.ProductInShoppingCartLowQuantityInWarehouseException;
import ru.yandex.practicum.model.error.SpecifiedProductAlreadyInWarehouseException;

import java.util.List;

@RestControllerAdvice
public class WarehouseExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ProductInShoppingCartLowQuantityInWarehouseException.class)
    public ProductInShoppingCartLowQuantityInWarehouseException handleException(
            ProductInShoppingCartLowQuantityInWarehouseException ex) {

        // Заполняем errorCause (соответствует JSON-полю "cause")
        if (ex.getErrorCause() == null) {
            ex.setErrorCause(new ProductInShoppingCartLowQuantityInWarehouseException.Cause());
        }
        ex.getErrorCause().setMessage("Причина ошибки");
        ex.getErrorCause().setStackTrace(List.of(
                new ProductInShoppingCartLowQuantityInWarehouseException.StackTraceElement(
                        "appClassLoader", "main", "1.0", "checkStock", "ShoppingCart.java", 42,
                        "ru.yandex.practicum.ShoppingCart", false)
        ));

        // Заполняем suppressedCauses (соответствует JSON-полю "suppressed")
        ex.setSuppressedCauses(List.of(
                new ProductInShoppingCartLowQuantityInWarehouseException.Cause(
                        List.of(new ProductInShoppingCartLowQuantityInWarehouseException.StackTraceElement(
                                "appClassLoader", "main", "1.0", "validate", "Validator.java", 15,
                                "ru.yandex.practicum.Validator", true
                        )),
                        "Подавленная причина",
                        "Локализованное сообщение подавленной причины"
                )
        ));

        // Заполняем jsonStackTrace (соответствует JSON-полю "stackTrace")
        ex.setJsonStackTrace(List.of(
                new ProductInShoppingCartLowQuantityInWarehouseException.StackTraceElement(
                        "appClassLoader", "main", "1.0", "handleException", "ShoppingCartExceptionHandler.java", 30,
                        "ru.yandex.practicum.ShoppingCartExceptionHandler", false)
        ));

        // Заполняем localizedMessage (соответствует JSON-полю "localizedMessage")
        ex.setLocalizedMessage("Локализованное сообщение основного исключения");

        // Гарантируем, что httpStatus и userMessage заполнены
        if (ex.getHttpStatus() == null) {
            ex.setHttpStatus("400 BAD_REQUEST");
        }
        if (ex.getUserMessage() == null) {
            ex.setUserMessage("Выбранный товар закончился или его количество меньше, чем вы указали в корзине.");
        }

        return ex;
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST) // 409 Conflict — подходит для "уже существует"
    @ExceptionHandler(SpecifiedProductAlreadyInWarehouseException.class)
    public SpecifiedProductAlreadyInWarehouseException handleException(
            SpecifiedProductAlreadyInWarehouseException ex) {

        // Заполняем errorCause (соответствует JSON-полю "cause")
        if (ex.getErrorCause() == null) {
            ex.setErrorCause(new SpecifiedProductAlreadyInWarehouseException.Cause());
        }
        ex.getErrorCause().setMessage("Указанный товар уже есть на складе");
        ex.getErrorCause().setStackTrace(List.of(
                new SpecifiedProductAlreadyInWarehouseException.StackTraceElement(
                        "appClassLoader", "main", "1.0", "checkWarehouse", "WarehouseService.java", 67,
                        "ru.yandex.practicum.WarehouseService", false)
        ));

        // Заполняем suppressedCauses (соответствует JSON-полю "suppressed")
        ex.setSuppressedCauses(List.of(
                new SpecifiedProductAlreadyInWarehouseException.Cause(
                        List.of(new SpecifiedProductAlreadyInWarehouseException.StackTraceElement(
                                "appClassLoader", "main", "1.0", "validate", "Validator.java", 23,
                                "ru.yandex.practicum.Validator", true
                        )),
                        "Дополнительная причина",
                        "Локализованное сообщение подавленной причины"
                )
        ));

        // Заполняем jsonStackTrace (соответствует JSON-полю "stackTrace")
        ex.setJsonStackTrace(List.of(
                new SpecifiedProductAlreadyInWarehouseException.StackTraceElement(
                        "appClassLoader", "main", "1.0", "handleException", "WarehouseExceptionHandler.java", 45,
                        "ru.yandex.practicum.WarehouseExceptionHandler", false)
        ));

        // Заполняем localizedMessage (соответствует JSON-полю "localizedMessage")
        ex.setLocalizedMessage("Товар уже присутствует на складе — дублирование запрещено");

        // Гарантируем, что httpStatus и userMessage заполнены
        if (ex.getHttpStatus() == null) {
            ex.setHttpStatus("400 BAD_REQUEST");
        }
        if (ex.getUserMessage() == null) {
            ex.setUserMessage("Товар с указанным идентификатором уже зарегистрирован на складе.");
        }

        return ex;
    }

    @ResponseStatus(HttpStatus.NOT_FOUND) // 404 Not Found
    @ExceptionHandler(NoSpecifiedProductInWarehouseException.class)
    public NoSpecifiedProductInWarehouseException handleException(
            NoSpecifiedProductInWarehouseException ex) {

        // Заполняем errorCause (соответствует JSON-полю "cause")
        if (ex.getErrorCause() == null) {
            ex.setErrorCause(new NoSpecifiedProductInWarehouseException.Cause());
        }
        ex.getErrorCause().setMessage("Указанный товар не найден на складе");
        ex.getErrorCause().setStackTrace(List.of(
                new NoSpecifiedProductInWarehouseException.StackTraceElement(
                        "appClassLoader", "main", "1.0", "findProduct", "WarehouseService.java", 88,
                        "ru.yandex.practicum.WarehouseService", false)
        ));

        // Заполняем suppressedCauses (соответствует JSON-полю "suppressed")
        ex.setSuppressedCauses(List.of(
                new NoSpecifiedProductInWarehouseException.Cause(
                        List.of(new NoSpecifiedProductInWarehouseException.StackTraceElement(
                                "appClassLoader", "main", "1.0", "validate", "Validator.java", 35,
                                "ru.yandex.practicum.Validator", true
                        )),
                        "Дополнительная причина отсутствия",
                        "Локализованное сообщение подавленной причины"
                )
        ));

        // Заполняем jsonStackTrace (соответствует JSON-полю "stackTrace")
        ex.setJsonStackTrace(List.of(
                new NoSpecifiedProductInWarehouseException.StackTraceElement(
                        "appClassLoader", "main", "1.0", "handleException", "WarehouseNotFoundExceptionHandler.java", 50,
                        "ru.yandex.practicum.WarehouseNotFoundExceptionHandler", false)
        ));

        // Заполняем localizedMessage (соответствует JSON-полю "localizedMessage")
        ex.setLocalizedMessage("Товар с указанным идентификатором отсутствует на складе");

        // Гарантируем, что httpStatus и userMessage заполнены
        if (ex.getHttpStatus() == null) {
        ex.setHttpStatus("400 BAD_REQUEST");
        }
        if (ex.getUserMessage() == null) {
            ex.setUserMessage("Товар с указанным ID не найден в системе склада.");
        }

        return ex;
    }
}
