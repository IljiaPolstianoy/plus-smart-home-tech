package ru.yandex.practicum.service;

import jakarta.persistence.PersistenceException;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.model.error.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.model.error.ProductInShoppingCartLowQuantityInWarehouseException;
import ru.yandex.practicum.model.error.SpecifiedProductAlreadyInWarehouseException;
import ru.yandex.practicum.model.shopping.ProductInCat;
import ru.yandex.practicum.model.shopping.ShoppingCartDto;
import ru.yandex.practicum.model.warehous.*;
import ru.yandex.practicum.storage.OrderBookingRepositor;
import ru.yandex.practicum.storage.ProductQuantityRepository;
import ru.yandex.practicum.storage.WareHouseRepository;

import java.math.BigDecimal;
import java.util.*;

@Service
public class WareHouseServiceImpl implements WareHouseService {

    private final WareHouseRepository wareHouseRepository;
    private final ProductQuantityRepository productQuantityRepository;
    private final OrderBookingRepositor orderBookingRepositor;

    public WareHouseServiceImpl(
            final WareHouseRepository wareHouseRepository,
            final ProductQuantityRepository productQuantityRepository, OrderBookingRepositor orderBookingRepositor) {
        this.wareHouseRepository = wareHouseRepository;
        this.productQuantityRepository = productQuantityRepository;
        this.orderBookingRepositor = orderBookingRepositor;
    }

    @Override
    public boolean addProductInWareHouse(final AddProductToWarehouseRequest addProductToWarehouseRequest) {
        final String productId = addProductToWarehouseRequest.getProductId();

        if (!wareHouseRepository.checkByProductId(productId)) {
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
            final ProductQuantity productQuantity = new ProductQuantity();
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
    public boolean returnProducts(final List<AddProductToWarehouseRequest> addProductToWarehouseRequestsList) {
        for (AddProductToWarehouseRequest addProductToWarehouseRequest : addProductToWarehouseRequestsList) {
            final ProductQuantity oldProductQuantity = productQuantityRepository
                    .findByProductQuantityId(addProductToWarehouseRequest.getProductId())
                    .orElseThrow((() -> new NoSpecifiedProductInWarehouseException(
                            "Продукта нет на складе",
                            "Продукта с id " + addProductToWarehouseRequest.getProductId() + " нет на складе",
                            "400 BAD_REQUEST"
                    )));
            oldProductQuantity.addQuantity(addProductToWarehouseRequest.getQuantity());
            productQuantityRepository.save(oldProductQuantity);
        }
        return true;
    }

    @Override
    public boolean shippedProductsInDelivery(final ShippedToDeliveryRequest shippedDeliveryRequest) {
        final OrderBooking orderBooking = orderBookingRepositor
                .findOrderBookingByOrderBookingId(shippedDeliveryRequest.getOrderId());
        orderBooking.setDeliveryId(shippedDeliveryRequest.getDeliveryId());
        orderBookingRepositor.save(orderBooking);
        return true;
    }

    @Override
    public BookedProductsDto assemblyProducts(final AssemblyProductsForOrderRequest assemblyProductsForOrderRequest) {

        final Map<String, Long> products = assemblyProductsForOrderRequest.getProducts();

        for (String productId : products.keySet()) {

            final ProductQuantity productQuantity = productQuantityRepository
                    .findByProductQuantityId(productId)
                    .orElseThrow((() -> new ProductInShoppingCartLowQuantityInWarehouseException(
                            "Товара не достаточно на складе.",
                            "Товар с id + " + productId + " не находится в требуем количестве на складе",
                            "400 BAD_REQUEST"
                    )));

            if (productQuantity.getQuantity() < products.get(productId)) {
                throw new ProductInShoppingCartLowQuantityInWarehouseException(
                        "Товара с id "
                                + productId + " не достаточно на складе.",
                        "Пожалуйста, уменьшите количество товара в корзине.",
                        "400 BAD_REQUEST"
                );
            }

            productQuantity.removeQuantity(products.get(productId));

            productQuantityRepository.save(productQuantity);

        }
        createOrderBookingFromRequest(assemblyProductsForOrderRequest);
        return createBookedProductsDto(products);
    }

    @Override
    public boolean create(final NewProductInWarehouseRequest newProductInWarehouseRequest) {
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
    public BookedProductsDto checkQuantity(final ShoppingCartDto shoppingCartDto) {

        BigDecimal deliveryWeight = BigDecimal.ZERO;
        BigDecimal deliveryVolume = BigDecimal.ZERO;
        boolean fragile = false;

        for (ProductInCat productInCat : shoppingCartDto.getProducts()) {

            final Optional<ProductQuantity> productQuantityOptional = productQuantityRepository
                    .findByProductQuantityId(productInCat.getProductId());
            final ProductQuantity productQuantity = getProductQuantityForCheck(productInCat, productQuantityOptional);

            if(productQuantity.getQuantity() < productInCat.getQuantity()) {
                throw new ProductInShoppingCartLowQuantityInWarehouseException(
                        "Товара с id "
                        + productInCat.getProductId() + " не достаточно на складе.",
                        "Пожалуйста, уменьшите количество товара в корзине.",
                        "400 BAD_REQUEST"
                );
            }

            final ProductsInWarehouse product = productQuantity.getProductsInWarehouse();

            final BigDecimal weightForThisProduct = product.getWeight()
                    .multiply(BigDecimal.valueOf(productQuantity.getQuantity()));

            deliveryWeight = deliveryWeight.add(weightForThisProduct);

            final BigDecimal volumeForThisProduct = product.getWidth()
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
            final ProductInCat productInCat,
            final Optional<ProductQuantity> productQuantityOptional
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

    private OrderBooking createOrderBookingFromRequest(final AssemblyProductsForOrderRequest request) {
        final OrderBooking orderBooking = OrderBooking.builder()
                .orderId(request.getOrderId())
                .deliveryId(null)
                .build();

        final Set<BookedProduct> bookedProducts = new HashSet<>();

        for (Map.Entry<String, Long> entry : request.getProducts().entrySet()) {
            String productId = entry.getKey();
            Long quantity = entry.getValue();

            final BookedProduct bookedProduct = new BookedProduct();

            bookedProduct.setOrderBooking(orderBooking);
            bookedProduct.setProductId(productId);
            bookedProduct.setQuantity(quantity);

            bookedProducts.add(bookedProduct);
        }

        orderBooking.setBookedProducts(bookedProducts);

        return orderBookingRepositor.save(orderBooking);
    }

    private BookedProductsDto createBookedProductsDto(final Map<String, Long> products) {
        BigDecimal deliveryWeight = BigDecimal.ZERO;
        BigDecimal deliveryVolume = BigDecimal.ZERO;
        boolean fragile = false;

        for (Map.Entry<String, Long> product : products.entrySet()) {
            final ProductsInWarehouse productsInWarehouse = wareHouseRepository.findByProductId(product.getKey());

            final BigDecimal weightForThisProduct = productsInWarehouse.getWeight()
                    .multiply(BigDecimal.valueOf(product.getValue()));

            deliveryWeight = deliveryWeight.add(weightForThisProduct);

            final BigDecimal volumeForThisProduct = productsInWarehouse.getWidth()
                    .multiply(productsInWarehouse.getHeight())
                    .multiply(productsInWarehouse.getDepth())
                    .multiply(BigDecimal.valueOf(product.getValue()));

            deliveryVolume = deliveryVolume.add(volumeForThisProduct);

            if (productsInWarehouse.isFragile()) {
                fragile = true;
            }
        }

        return BookedProductsDto.builder()
                .deliveryWeight(deliveryWeight)
                .deliveryVolume(deliveryVolume)
                .fragile(fragile)
                .build();
    }
}


