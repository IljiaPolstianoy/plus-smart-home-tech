package ru.yandex.practicum.model.warehous;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AddProductToWarehouseRequest {

    private String productId;

    @NotNull
    @Min(1)
    private long quantity;
}
