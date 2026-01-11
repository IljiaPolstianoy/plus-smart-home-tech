package ru.yandex.practicum.model.warehous;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class BookedProductsDto {

    @NotNull
    private BigDecimal deliveryWeight;

    @NotNull
    private BigDecimal deliveryVolume;

    @NotNull
    private boolean fragile;
}
