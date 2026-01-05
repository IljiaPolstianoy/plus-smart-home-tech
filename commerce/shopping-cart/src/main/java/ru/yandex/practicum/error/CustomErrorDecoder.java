package ru.yandex.practicum.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.model.error.ProductInShoppingCartLowQuantityInWarehouseException;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class CustomErrorDecoder implements ErrorDecoder {


    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ErrorDecoder defaultDecoder = new Default();


    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 400 && response.body() != null) {
            try {
                String responseBody = new String(
                        response.body().asInputStream().readAllBytes(),
                        StandardCharsets.UTF_8
                );

                log.debug("Warehouse 400 response body: {}", responseBody);

                ProductInShoppingCartLowQuantityInWarehouseException ex =
                        objectMapper.readValue(
                                responseBody,
                                ProductInShoppingCartLowQuantityInWarehouseException.class
                        );

                return ex;

            } catch (Exception e) {
                log.error("Failed to parse full exception from warehouse response: {}", e.getMessage(), e);
                return new ProductInShoppingCartLowQuantityInWarehouseException(
                        "Ошибка парсинга ответа склада",
                        "Не удалось обработать ответ сервера.",
                        "502 BAD_GATEWAY"
                );
            }
        }

        return defaultDecoder.decode(methodKey, response);
    }
}
