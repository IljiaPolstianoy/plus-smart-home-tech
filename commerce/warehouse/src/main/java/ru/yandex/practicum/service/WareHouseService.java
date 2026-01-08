package ru.yandex.practicum.service;

import ru.yandex.practicum.model.shopping.ShoppingCartDto;
import ru.yandex.practicum.model.warehous.AddProductToWarehouseRequest;
import ru.yandex.practicum.model.warehous.AddressDto;
import ru.yandex.practicum.model.warehous.BookedProductsDto;
import ru.yandex.practicum.model.warehous.NewProductInWarehouseRequest;

public interface WareHouseService {

    boolean create(NewProductInWarehouseRequest newProductInWarehouseRequest);

    BookedProductsDto checkQuantity(ShoppingCartDto shoppingCartDto);

    boolean addProductInWareHouse(AddProductToWarehouseRequest addProductToWarehouseRequest);

    AddressDto getAddress();
}
