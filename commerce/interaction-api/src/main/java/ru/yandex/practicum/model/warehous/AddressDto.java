package ru.yandex.practicum.model.warehous;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
public class AddressDto {

    private String country;
    private String city;
    private String street;
    private String house;
    private String flat;
}
