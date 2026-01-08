package ru.yandex.practicum.model.warehous;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
public class AddProductToWarehouseRequest {

    private String productId;

    @NotNull
    @Min(1)
    private long quantity;
}
