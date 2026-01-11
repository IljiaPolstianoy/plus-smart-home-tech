package ru.yandex.practicum.model.delivery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "address")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Address {
    @Id
    @Column(name = "address_id", nullable = false, length = Integer.MAX_VALUE)
    private String addressId;

    @Column(name = "country", length = Integer.MAX_VALUE)
    private String country;

    @Column(name = "city", length = Integer.MAX_VALUE)
    private String city;

    @Column(name = "street", length = Integer.MAX_VALUE)
    private String street;

    @Column(name = "house", length = Integer.MAX_VALUE)
    private String house;

    @Column(name = "flat", length = Integer.MAX_VALUE)
    private String flat;

}