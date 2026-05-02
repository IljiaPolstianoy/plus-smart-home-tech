package ru.yandex.practicum.model.warehous;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
public class AssemblyProductsForOrderRequest {

    @NotBlank
    private String orderId;

    @NotEmpty
    private Map<String, Long> products;
}
