package ru.yandex.practicum.model.warehous;

import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
public class DimensionDto {

    @Min(1)
    private BigDecimal width;

    @Min(1)
    private BigDecimal height;

    @Min(1)
    private BigDecimal depth;
}
