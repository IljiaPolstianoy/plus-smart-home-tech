package ru.yandex.practicum.service;

import ru.yandex.practicum.shopping.ShoppingCartDto;
import ru.yandex.practicum.warehous.AddProductToWarehouseRequest;
import ru.yandex.practicum.warehous.AddressDto;
import ru.yandex.practicum.warehous.BookedProductsDto;
import ru.yandex.practicum.warehous.NewProductInWarehouseRequest;

public interface WareHouseService {

    boolean create(NewProductInWarehouseRequest newProductInWarehouseRequest);

    BookedProductsDto checkQuantity(ShoppingCartDto shoppingCartDto);

    boolean addProductInWareHouse(AddProductToWarehouseRequest addProductToWarehouseRequest);

    AddressDto getAddress();
}
