package ru.yandex.practicum;

import ru.yandex.practicum.model.delivery.Address;
import ru.yandex.practicum.model.warehous.AddressDto;

public class DeliveryMapper {

    public static Address fromAddressDto(AddressDto addressDto) {
        return Address.builder()
                .country(addressDto.getCountry())
                .city(addressDto.getCity())
                .street(addressDto.getStreet())
                .house(addressDto.getHouse())
                .flat(addressDto.getFlat())
                .build();
    }
}
