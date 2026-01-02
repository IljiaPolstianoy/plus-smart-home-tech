package ru.yandex.practicum;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.feign.WarehouseFeignClient;
import ru.yandex.practicum.service.WareHouseService;
import ru.yandex.practicum.model.shopping.ShoppingCartDto;
import ru.yandex.practicum.model.warehous.AddProductToWarehouseRequest;
import ru.yandex.practicum.model.warehous.AddressDto;
import ru.yandex.practicum.model.warehous.BookedProductsDto;
import ru.yandex.practicum.model.warehous.NewProductInWarehouseRequest;

@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
@Validated
public class WareHouseController implements WarehouseFeignClient {

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
