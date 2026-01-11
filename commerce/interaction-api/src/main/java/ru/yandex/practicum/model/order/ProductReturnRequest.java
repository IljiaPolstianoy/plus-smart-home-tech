package ru.yandex.practicum.model.order;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Setter
@Getter
@ToString
@Builder(toBuilder = true)
public class ProductReturnRequest {

    private String orderId;

    @NotNull
    private Map<String, Long> products;
}
