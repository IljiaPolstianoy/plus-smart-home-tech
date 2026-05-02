package ru.yandex.practicum.model.constant;

import java.math.BigDecimal;

public class DeliveryDefault {
    public static final BigDecimal BASE_COST = BigDecimal.valueOf(5.0);
    public static final BigDecimal FRAGILE_MULTIPLIER = BigDecimal.valueOf(0.2);
    public static final BigDecimal WEIGHT_MULTIPLIER = BigDecimal.valueOf(0.3);
    public static final BigDecimal VOLUME_MULTIPLIER = BigDecimal.valueOf(0.2);
    public static final BigDecimal ADDRESS_SURCHARGE_MULTIPLIER = BigDecimal.valueOf(0.2);
}
