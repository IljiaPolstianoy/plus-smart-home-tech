package ru.yandex.practicum.model.warehous;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Builder
public class ShippedToDeliveryRequest {

    @NotBlank
    private String orderId;

    @NotBlank
    private String deliveryId;
}
