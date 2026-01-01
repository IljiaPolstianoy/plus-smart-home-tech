package ru.yandex.practicum;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.service.WareHouseService;
import ru.yandex.practicum.shopping.ShoppingCartDto;
import ru.yandex.practicum.warehous.AddProductToWarehouseRequest;
import ru.yandex.practicum.warehous.AddressDto;
import ru.yandex.practicum.warehous.BookedProductsDto;
import ru.yandex.practicum.warehous.NewProductInWarehouseRequest;

@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
@Validated
public class WareHouseController {

    private final WareHouseService wareHouseService;

    @PutMapping
    public boolean create(@RequestBody NewProductInWarehouseRequest newProductInWarehouseRequest) {
        return wareHouseService.create(newProductInWarehouseRequest);
    }

    @PostMapping("/check")
    public BookedProductsDto checkQuantity(@RequestBody ShoppingCartDto shoppingCartDto) {
        return wareHouseService.checkQuantity(shoppingCartDto);
    }

    @PostMapping("/add")
    public boolean addProductOnWareHouse(@RequestBody AddProductToWarehouseRequest addProductToWarehouseRequest) {
        return wareHouseService.addProductInWareHouse(addProductToWarehouseRequest);
    }

    @GetMapping("/address")
    public AddressDto getAddress() {
        return wareHouseService.getAddress();
    }
}
