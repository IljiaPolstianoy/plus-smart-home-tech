package ru.yandex.practicum.warehous;

import java.security.SecureRandom;
import java.util.Random;

public class WarehouseAddress {

    private static final String[] ADDRESSES =
            new String[] {"ADDRESS_1", "ADDRESS_2"};

    public static final String CURRENT_ADDRESS =
            ADDRESSES[Random.from(new SecureRandom()).nextInt(0, ADDRESSES.length)];}
