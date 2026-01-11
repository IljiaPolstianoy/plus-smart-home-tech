package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.PaymentMapper;
import ru.yandex.practicum.PaymentRepository;
import ru.yandex.practicum.feign.OrderFeignClient;
import ru.yandex.practicum.feign.ShoppingStoreFeignClient;
import ru.yandex.practicum.model.constant.LogDefault;
import ru.yandex.practicum.model.error.NoPaymentFoundException;
import ru.yandex.practicum.model.error.NotEnoughInfoInOrderToCalculateException;
import ru.yandex.practicum.model.order.OrderDto;
import ru.yandex.practicum.model.payment.Payment;
import ru.yandex.practicum.model.payment.PaymentDto;
import ru.yandex.practicum.model.payment.State;
import ru.yandex.practicum.model.product.ProductDto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final ShoppingStoreFeignClient shoppingStoreFeignClient;
    private final OrderFeignClient orderFeignClient;

    @Override
    public PaymentDto createPayment(final OrderDto orderDto) {
        log.info("Creating Payment for order {}", orderDto);
        final Payment payment = Payment.builder()
                .totalProduct(getProductCost(orderDto))
                .state(State.PENDING)
                .freeTotal(getFreeTotal(orderDto))
                .build();
        log.info("Запрос на сохранение в базе данных объекта Payment.");
        return PaymentMapper.toPaymentDto(paymentRepository.save(payment));
    }

    @Override
    public BigDecimal getTotalCost(final OrderDto orderDto) {
        log.info("Расчет общей суммы для заказа с id {}", orderDto);
        return getProductCost(orderDto).add(orderDto.getDeliveryPrice()).add(getFreeTotal(orderDto));
    }

    @Override
    public void refund(final String orderId) {
        log.info("Метод для успешной оплаты.");
        log.trace(LogDefault.GET_OBJECT_FROM_BD, Payment.class, orderId);
        final Optional<Payment> payment = paymentRepository.findByOrderId(orderId);

        if (payment.isPresent()) {
            log.info("Изменение статуса у объекта Payment с id {} yf SUCCESS", payment.get().getPaymentId());
            payment.get().setState(State.SUCCESS);
            orderFeignClient.paymentOrder(payment.get().getOrderId());
            log.info(LogDefault.SEND_OBJECT_IN_BD, Payment.class, payment.get().getPaymentId());
            paymentRepository.save(payment.get());
        }

        throw new NoPaymentFoundException(
                "Платеж не найден",
                "Платеж с id + " + payment.get().getPaymentId() + " не найден",
                HttpStatus.NOT_FOUND
        );
    }

    @Override
    public BigDecimal getProductCost(final OrderDto orderDto) {
        log.info("Метод расчета полной стоимости всех товаров. ");
        log.trace("Проверка, что все необходимые поля заполнены");
        if (orderDto.getProducts().isEmpty()) {
            throw new NotEnoughInfoInOrderToCalculateException(
                    "Недостаточно информации для расчета стоимости.",
                    "Товары отсутствуют в корзине.",
                    HttpStatus.BAD_REQUEST
            );
        }
        log.trace("Получение списка всех товаров.");
        final Map<String, Long> products = orderDto.getProducts();
        BigDecimal productCoat = BigDecimal.ZERO;

        log.trace("Расчет полной стоимости всех продуктов.");
        for (Map.Entry<String, Long> product : products.entrySet()) {
            final ProductDto productDto = shoppingStoreFeignClient.getProduct(product.getKey());
            productCoat = productCoat.add(BigDecimal.valueOf(productDto.getPrice() * product.getValue()));
        }
        return productCoat;
    }

    @Override
    public void failed(final String orderId) {
        log.info("Метод для не успешной оплаты.");
        log.trace(LogDefault.GET_OBJECT_FROM_BD, Payment.class, orderId);
        final Optional<Payment> payment = paymentRepository.findByOrderId(orderId);

        if (payment.isPresent()) {
            log.info("Изменение статуса у объекта Payment с id {} yf SUCCESS", payment.get().getPaymentId());
            payment.get().setState(State.FAILED);
            orderFeignClient.paymentFailedOrder(payment.get().getOrderId());
            log.info(LogDefault.SEND_OBJECT_IN_BD, Payment.class, payment.get().getPaymentId());
            paymentRepository.save(payment.get());
        }

        throw new NoPaymentFoundException(
                "Платеж не найден",
                "Платеж с id + " + payment.get().getPaymentId() + " не найден",
                HttpStatus.NOT_FOUND
        );
    }

    private BigDecimal getFreeTotal(final OrderDto orderDto) {
        return getProductCost(orderDto).multiply(BigDecimal.valueOf(0.1));
    }
}
