package ru.yandex.practicum.model.order;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.yandex.practicum.model.shopping.ShoppingCartDto;
import ru.yandex.practicum.model.warehous.AddressDto;

@Setter
@Getter
@ToString
@Builder(toBuilder=true)
public class CreateNewOrderRequest {

    @NotNull
    private ShoppingCartDto shoppingCartId;

    @NotNull
    private AddressDto deliveryAddress;
}