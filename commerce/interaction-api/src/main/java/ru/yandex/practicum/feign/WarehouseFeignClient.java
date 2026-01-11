package ru.yandex.practicum.feign;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.model.shopping.ShoppingCartDto;
import ru.yandex.practicum.model.warehous.*;

import java.util.List;

@FeignClient(name = "warehouse")
public interface WarehouseFeignClient {

    @PutMapping("/api/v1/warehouse")
    boolean create(@RequestBody NewProductInWarehouseRequest request);

    @PostMapping("/api/v1/warehouse/check")
    BookedProductsDto checkQuantity(@RequestBody ShoppingCartDto shoppingCartDto);

    @PostMapping("/api/v1/warehouse/add")
    boolean addProductOnWareHouse(@RequestBody AddProductToWarehouseRequest request);

    @GetMapping("/api/v1/warehouse/address")
    AddressDto getAddress();

    @PostMapping("/api/v1/warehouse/shipped")
    boolean shippedProductsInDelivery(@RequestBody @Valid ShippedToDeliveryRequest shippedToDeliveryRequest);

    @PostMapping("/api/v1/warehouse/return")
    boolean returnProducts(
            @RequestBody @Valid final List<AddProductToWarehouseRequest> addProductToWarehouseRequest
    );

    @PostMapping("/api/v1/warehouse/assembly")
    BookedProductsDto assemblyProducts(
            @RequestBody @Valid final AssemblyProductsForOrderRequest assemblyProductsForOrderRequest
    );
}