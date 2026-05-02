package ru.yandex.practicum.service;

import ru.yandex.practicum.model.shopping.ShoppingCartDto;
import ru.yandex.practicum.model.warehous.*;

import java.util.List;

public interface WareHouseService {

    boolean create(NewProductInWarehouseRequest newProductInWarehouseRequest);

    BookedProductsDto checkQuantity(ShoppingCartDto shoppingCartDto);

    boolean addProductInWareHouse(AddProductToWarehouseRequest addProductToWarehouseRequest);

    AddressDto getAddress();

    boolean returnProducts(List<AddProductToWarehouseRequest> addProductToWarehouseRequest);

    boolean shippedProductsInDelivery(ShippedToDeliveryRequest shippedDeliveryRequest);

    BookedProductsDto assemblyProducts(AssemblyProductsForOrderRequest assemblyProductsForOrderRequest);
}
