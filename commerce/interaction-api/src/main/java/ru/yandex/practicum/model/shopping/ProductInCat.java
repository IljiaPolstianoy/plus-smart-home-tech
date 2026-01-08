package ru.yandex.practicum.model.shopping;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductInCat {

    @NotBlank
    private String productId;

    @NotBlank
    private Long quantity;
}
