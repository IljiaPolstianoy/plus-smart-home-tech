package ru.yandex.practicum.warehous;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
public class NewProductInWarehouseRequest {

    @NotBlank
    private String productId;

    private boolean fragile;

    @NotNull
    private DimensionDto dimension;

    @Min(1)
    private BigDecimal weight;
}
