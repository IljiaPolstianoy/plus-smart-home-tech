package ru.yandex.practicum.model.quantity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChangeProductQuantityRequest {

    @NotBlank
    private String productId;

    @NotNull
    private long quantity;

}
