package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.feign.DeliveryFeignClient;
import ru.yandex.practicum.feign.PaymentFeignClient;
import ru.yandex.practicum.feign.WarehouseFeignClient;
import ru.yandex.practicum.model.constant.LogDefault;
import ru.yandex.practicum.model.constant.PageableDefault;
import ru.yandex.practicum.model.delivery.DeliveryDto;
import ru.yandex.practicum.model.delivery.DeliveryState;
import ru.yandex.practicum.model.error.NoOrderFoundException;
import ru.yandex.practicum.model.order.*;
import ru.yandex.practicum.model.payment.PaymentDto;
import ru.yandex.practicum.model.shopping.ProductInCat;
import ru.yandex.practicum.model.shopping.ShoppingCartDto;
import ru.yandex.practicum.model.warehous.AddProductToWarehouseRequest;
import ru.yandex.practicum.model.warehous.AssemblyProductsForOrderRequest;
import ru.yandex.practicum.model.warehous.BookedProductsDto;
import ru.yandex.practicum.storage.OrderRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final WarehouseFeignClient warehouseFeignClient;
    private final DeliveryFeignClient deliveryFeignClient;
    private final PaymentFeignClient paymentFeignClient;

    @Override
    public Page<OrderDto> findAllOrders(final String userName) {
        log.info("Создание дефолтного Pageable");
        final Pageable pageable = PageRequest.of(
                PageableDefault.DEFAULT_PAGE,
                PageableDefault.DEFAULT_SIZE,
                PageableDefault.DEFAULT_DIRECTION,
                PageableDefault.DEFAULT_SORT_BY
        );

        log.info("Отправка запроса на получение всех заказов для пользователя {}", userName);
        final Page<OrderDto> orderDtoPage = orderRepository.findAllByUserName(userName, pageable);
        log.info("Получены все заказы пользователя {} с пагинацией", userName);
        return orderDtoPage;
    }

    @Override
    public OrderDto createOrder(final CreateNewOrderRequest createNewOrderRequest) {
        log.info("Сохранение нового заказа в базу данных.");
        log.trace("Получение корзины из запроса.");
        final ShoppingCartDto shoppingCartDto = createNewOrderRequest.getShoppingCartId();
        log.trace("Получение веса, объема и хрупкости из запроса.");
        final BookedProductsDto bookedProductsDto = warehouseFeignClient.checkQuantity(shoppingCartDto);
        log.trace("Получение списка продуктов из запроса.");
        final Map<String, Long> products = getProducts(shoppingCartDto.getProducts());
        log.info("Создание заказа с статусом NEW.");
        final OrderDto orderDto = OrderDto.builder()
                .shoppingCartId(shoppingCartDto.getShoppingCartId())
                .products(products)
                .state(State.NEW)
                .deliveryWeight(bookedProductsDto.getDeliveryWeight())
                .deliveryVolume(bookedProductsDto.getDeliveryVolume())
                .fragile(bookedProductsDto.isFragile())
                .build();
        orderDto.setProductPrice(paymentFeignClient.getProductCost(orderDto));

        log.trace("Отправка запроса на сохранение в базу данных объекта OrderDto.");
        orderRepository.save(orderDto);
        log.info("Заказ {} успешно сохранен.", orderDto.getOrderId());

        log.info("Создание доставки для заказа с id {}", orderDto.getOrderId());
        log.trace("Создание объекта DeliveryDto");
        DeliveryDto deliveryDto = DeliveryDto.builder()
                .deliveryState(DeliveryState.CREATED)
                .fromAddress(warehouseFeignClient.getAddress())
                .toAddress(createNewOrderRequest.getDeliveryAddress())
                .orderId(orderDto.getOrderId())
                .build();
        log.trace("Отправка запроса на сохранение в базу данных объекта DeliveryDto.");
        deliveryDto = deliveryFeignClient.createDelivery(deliveryDto);
        orderDto.setDeliveryId(deliveryDto.getOrderId());
        log.trace("Установлен для OrderDto с id {} id доставки.", orderDto.getOrderId());

        log.info("Создание платежа для заказа с id {}", orderDto.getOrderId());
        log.trace("Создание объекта PaymentDto.");
        PaymentDto paymentDto = paymentFeignClient.createPayment(orderDto);
        orderDto.setPaymentId(paymentDto.getPaymentId());
        orderDto.setProductPrice(paymentFeignClient.getProductCost(orderDto));

        log.trace("Отправка запроса на сохранение в базу данных объекта OrderDto с обновленными данными.");
        orderRepository.save(orderDto);
        log.info("Заказ {} успешно сохранен.", orderDto.getOrderId());
        return orderDto;
    }

    @Override
    public OrderDto returnOrder(final ProductReturnRequest productReturnRequest) {
        log.info("Возврат товаров из заказа");
        final List<AddProductToWarehouseRequest> addProductToWarehouseRequests = new ArrayList<>();
        productReturnRequest
                .getProducts()
                .forEach((String productId, Long quantity) -> addProductToWarehouseRequests.add(new AddProductToWarehouseRequest(productId, quantity)));
        warehouseFeignClient.returnProducts(addProductToWarehouseRequests);
        log.trace("Количество товаров на складе увеличено.");

        final OrderDto orderDto = getOrderDto(productReturnRequest.getOrderId());
        productReturnRequest.getProducts().forEach((String productId, Long quantity) -> orderDto.getProducts().remove(productId));
        log.trace("Количество товаров в заказе уменьшено.");
        return orderRepository.save(orderDto);
    }

    @Override
    public OrderDto paymentOrder(final String orderId) {
        return updateStatus(orderId, State.PAID);
    }

    @Override
    public OrderDto paymentFailedOrder(final String orderId) {
        return updateStatus(orderId, State.PAYMENT_FAILED);
    }

    @Override
    public OrderDto deliveryOrder(final String orderId) {
        return updateStatus(orderId, State.DELIVERED);
    }

    @Override
    public OrderDto deliveryFailedOrder(final String orderId) {
        return updateStatus(orderId, State.DELIVERY_FAILED);
    }

    @Override
    public OrderDto completedOrder(final String orderId) {
        return updateStatus(orderId, State.COMPLETED);
    }

    @Override
    public OrderDto calculateTotal(final String orderId) {
        log.info("Расчет стоимости заказа.");
        log.trace(LogDefault.GET_OBJECT_FROM_BD, OrderDto.class, orderId);
        OrderDto orderDto = getOrderDto(orderId);
        log.trace("Получение общей стоимости заказа.");
        final BigDecimal totalPrice = paymentFeignClient.getTotalCost(orderDto);
        orderDto.setTotalPrice(totalPrice);
        orderDto = updateStatus(orderId, State.ON_PAYMENT);
        log.info(
                "Отправка запроса на сохранение в базу данных" +
                        " объекта OrderDto с id {} с обновленной информацией суммой заказа",
                orderId
        );
        return orderRepository.save(orderDto);
    }

    @Override
    public OrderDto calculateDeliveryOrder(final String orderId) {
        log.info("Расчет стоимости доставки.");
        log.trace(LogDefault.GET_OBJECT_FROM_BD, OrderDto.class, orderId);
        OrderDto orderDto = getOrderDto(orderId);
        log.trace("Получение общей стоимости доставки.");
        final BigDecimal deliveryTotalPrice = deliveryFeignClient.cost(orderDto);
        orderDto.setDeliveryPrice(deliveryTotalPrice);
        orderDto = updateStatus(orderId, State.ON_DELIVERY);
        log.info(
                "Отправка запроса на сохранение в базу данных" +
                        " объекта OrderDto с id {} с обновленной информацией суммой доставки",
                orderId
        );
        return orderRepository.save(orderDto);
    }

    @Override
    public OrderDto assemblyOrder(final String orderId) {
        log.info("Сборка заказа.");
        log.trace(LogDefault.GET_OBJECT_FROM_BD, OrderDto.class, orderId);
        final OrderDto orderDto = getOrderDto(orderId);
        log.trace("Получение списка товаров");
        final Map<String, Long> products = orderDto.getProducts();
        log.trace("Создание объекта AssemblyProductsForOrderRequest");
        final AssemblyProductsForOrderRequest assemblyProductsForOrderRequest = AssemblyProductsForOrderRequest.builder()
                .orderId(orderId)
                .products(products)
                .build();

        warehouseFeignClient.assemblyProducts(assemblyProductsForOrderRequest);
        updateStatus(orderId, State.ASSEMBLED);
        log.info(
                "Отправка запроса на сохранение в базу данных" +
                        " объекта OrderDto с id {} с обновленным статусом",
                orderId
        );
        return orderRepository.save(orderDto);
    }

    @Override
    public OrderDto assemblyFailedOrder(final String orderId) {
        return updateStatus(orderId, State.ASSEMBLY_FAILED);
    }

    private Map<String, Long> getProducts(final List<ProductInCat> productInCats) {
        return productInCats.stream()
                .collect(Collectors.toMap(
                        ProductInCat::getProductId,
                        ProductInCat::getQuantity
                ));
    }

    private OrderDto getOrderDto(final String orderId) {
        return orderRepository.findByOrderId(orderId).orElseThrow(() -> new NoOrderFoundException(
                "Заказ не найден.",
                "Заказа с id + " + orderId + " не найден.",
                HttpStatus.BAD_REQUEST
        ));
    }

    private OrderDto updateStatus(final String orderId, final State state) {
        log.info("Установка нового статуса для заказа с {} .", orderId);
        final OrderDto oldOrderDto = getOrderDto(orderId);

        if (oldOrderDto.getState().canTransitionTo(state)) {
            oldOrderDto.setState(state);
            return orderRepository.save(oldOrderDto);
        }

        throw new IncorrectStatus(
                "Статус "
                        + state
                        + " не может быть присвоен заказу с статусом "
                        + oldOrderDto.getState());
    }
}
