package ru.yandex.practicum;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.feign.WarehouseFeignClient;
import ru.yandex.practicum.model.shopping.ShoppingCartDto;
import ru.yandex.practicum.model.warehous.*;
import ru.yandex.practicum.service.WareHouseService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
@Validated
public class WareHouseController implements WarehouseFeignClient {

    private final WareHouseService wareHouseService;

    @PutMapping
    public boolean create(@RequestBody final NewProductInWarehouseRequest newProductInWarehouseRequest) {
        return wareHouseService.create(newProductInWarehouseRequest);
    }

    @PostMapping("/shipped")
    public boolean shippedProductsInDelivery(@RequestBody @Valid final ShippedToDeliveryRequest shippedToDeliveryRequest) {
        return wareHouseService.shippedProductsInDelivery(shippedToDeliveryRequest);
    }

    @PostMapping("/return")
    public boolean returnProducts(
            @RequestBody @Valid final List<AddProductToWarehouseRequest> addProductToWarehouseRequest
    ) {
        return wareHouseService.returnProducts(addProductToWarehouseRequest);
    }

    @PostMapping("/assembly")
    public BookedProductsDto assemblyProducts(
            @RequestBody @Valid final AssemblyProductsForOrderRequest assemblyProductsForOrderRequest
    ) {
        return wareHouseService.assemblyProducts(assemblyProductsForOrderRequest);
    }

    @PostMapping("/check")
    public BookedProductsDto checkQuantity(@RequestBody final ShoppingCartDto shoppingCartDto) {
        return wareHouseService.checkQuantity(shoppingCartDto);
    }

    @PostMapping("/add")
    public boolean addProductOnWareHouse(@RequestBody final AddProductToWarehouseRequest addProductToWarehouseRequest) {
        return wareHouseService.addProductInWareHouse(addProductToWarehouseRequest);
    }

    @GetMapping("/address")
    public AddressDto getAddress() {
        return wareHouseService.getAddress();
    }
}
