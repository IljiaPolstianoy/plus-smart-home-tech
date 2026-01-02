package ru.yandex.practicum.service;

import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.model.error.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.model.error.ProductInShoppingCartLowQuantityInWarehouseException;
import ru.yandex.practicum.model.error.SpecifiedProductAlreadyInWarehouseException;
import ru.yandex.practicum.model.shopping.ProductInCat;
import ru.yandex.practicum.model.shopping.ShoppingCartDto;
import ru.yandex.practicum.model.warehous.*;
import ru.yandex.practicum.storage.ProductQuantityRepository;
import ru.yandex.practicum.storage.WareHouseRepository;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class WareHouseServiceImpl implements WareHouseService {

    private final WareHouseRepository wareHouseRepository;
    private final ProductQuantityRepository productQuantityRepository;

    public WareHouseServiceImpl(WareHouseRepository wareHouseRepository, ProductQuantityRepository productQuantityRepository) {
        this.wareHouseRepository = wareHouseRepository;
        this.productQuantityRepository = productQuantityRepository;
    }

    @Override
    public boolean addProductInWareHouse(AddProductToWarehouseRequest addProductToWarehouseRequest) {
        final String productId = addProductToWarehouseRequest.getProductId();

        if (!wareHouseRepository.findByProductId(productId)) {
            throw new NoSpecifiedProductInWarehouseException(
                    "Продукт с ID " + productId + " не найден",
                    "Указанный продукт отсутствует на складе. Проверьте ID.",
                    "400 BAD_REQUEST"
            );
        }

        final Optional<ProductQuantity> productQuantityOption = productQuantityRepository.findByProductQuantityId(productId);
        if (productQuantityOption.isPresent()) {
            productQuantityOption.get().addQuantity(addProductToWarehouseRequest.getQuantity());
            productQuantityRepository.save(productQuantityOption.get());
        } else {
            ProductQuantity productQuantity = new ProductQuantity();
            productQuantity.setProductQuantityId(productId);
            productQuantity.setQuantity(addProductToWarehouseRequest.getQuantity());
            productQuantityRepository.save(productQuantity);
        }
        return true;
    }

    @Override
    public AddressDto getAddress() {
        return AddressDto.builder()
                .country(WarehouseAddress.CURRENT_ADDRESS)
                .city(WarehouseAddress.CURRENT_ADDRESS)
                .street(WarehouseAddress.CURRENT_ADDRESS)
                .house(WarehouseAddress.CURRENT_ADDRESS)
                .flat(WarehouseAddress.CURRENT_ADDRESS)
                .build();
    }

    @Override
    public boolean create(NewProductInWarehouseRequest newProductInWarehouseRequest) {
        final ProductsInWarehouse productsInWarehouse = ProductsInWarehouse.builder()
                .productId(newProductInWarehouseRequest.getProductId())
                .fragile(newProductInWarehouseRequest.isFragile())
                .width(newProductInWarehouseRequest.getDimension().getWidth())
                .height(newProductInWarehouseRequest.getDimension().getHeight())
                .depth(newProductInWarehouseRequest.getDimension().getDepth())
                .weight(newProductInWarehouseRequest.getWeight())
                .build();

        try {
            wareHouseRepository.save(productsInWarehouse);
        } catch (PersistenceException e) {
            throw new SpecifiedProductAlreadyInWarehouseException(
                    "Продукт с ID " + newProductInWarehouseRequest.getProductId() + " уже находится на складе",
                    "Данный продукт уже зарегистрирован на складе. Проверьте данные.",
                    "400 BAD_REQUEST"
            );
        }
        return true;
    }

    @Override
    public BookedProductsDto checkQuantity(ShoppingCartDto shoppingCartDto) {

        BigDecimal deliveryWeight = BigDecimal.ZERO;
        BigDecimal deliveryVolume = BigDecimal.ZERO;
        boolean fragile = false;

        for (ProductInCat productInCat : shoppingCartDto.getProducts()) {

            final Optional<ProductQuantity> productQuantityOptional = productQuantityRepository
                    .findByProductQuantityId(productInCat.getProductId());
            final ProductQuantity productQuantity = getProductQuantityForCheck(productInCat, productQuantityOptional);

            if(productQuantity.getQuantity() < productInCat.getQuantity()) {
                throw new ProductInShoppingCartLowQuantityInWarehouseException("Товара с id "
                        + productInCat.getProductId() + " не достаточно на складе.",
                        "Пожалуйста, уменьшите количество товара в корзине.",
                        "400 BAD_REQUEST"
                );
            }

            final ProductsInWarehouse product = productQuantity.getProductsInWarehouse();

            BigDecimal weightForThisProduct = product.getWeight()
                    .multiply(BigDecimal.valueOf(productQuantity.getQuantity()));

            deliveryWeight = deliveryWeight.add(weightForThisProduct);

            BigDecimal volumeForThisProduct = product.getWidth()
                    .multiply(product.getHeight())
                    .multiply(product.getDepth())
                    .multiply(BigDecimal.valueOf(productQuantity.getQuantity()));

            deliveryVolume = deliveryVolume.add(volumeForThisProduct);

            if (product.isFragile()) {
                fragile = true;
            }
        }

        return BookedProductsDto.builder()
                .deliveryWeight(deliveryWeight)
                .deliveryVolume(deliveryVolume)
                .fragile(fragile)
                .build();
    }

    private ProductQuantity getProductQuantityForCheck(
            ProductInCat productInCat,
            Optional<ProductQuantity> productQuantityOptional
    ) {
        if (
                productQuantityOptional.isPresent()
                        && productQuantityOptional.get().getQuantity() >= productInCat.getQuantity()
        ) {
            return productQuantityOptional.get();
        } else {
            throw new ProductInShoppingCartLowQuantityInWarehouseException(
                    "Недостаточно товара на складе",
                    "Выбранный товар закончился или его количество меньше, чем вы указали в корзине.",
                    "400 BAD_REQUEST"
            );
        }
    }
}


