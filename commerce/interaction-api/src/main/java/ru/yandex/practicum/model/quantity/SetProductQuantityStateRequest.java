package ru.yandex.practicum.model.quantity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class SetProductQuantityStateRequest {

    @NotBlank
    private String productId;

    @NotNull
    private QuantityState quantityState;
}
