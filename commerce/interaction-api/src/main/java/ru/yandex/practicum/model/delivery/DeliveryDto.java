package ru.yandex.practicum.model.delivery;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.yandex.practicum.model.warehous.AddressDto;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
public class DeliveryDto {

    private String deliveryId;

    @NotNull
    private AddressDto fromAddress;

    @NotNull
    private AddressDto toAddress;

    @NotBlank
    private String orderId;

    @NotNull
    private DeliveryState deliveryState;
}
