package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.DeliveryMapper;
import ru.yandex.practicum.DeliveryRepository;
import ru.yandex.practicum.feign.OrderFeignClient;
import ru.yandex.practicum.feign.WarehouseFeignClient;
import ru.yandex.practicum.model.constant.DeliveryDefault;
import ru.yandex.practicum.model.delivery.Delivery;
import ru.yandex.practicum.model.delivery.DeliveryDto;
import ru.yandex.practicum.model.delivery.DeliveryState;
import ru.yandex.practicum.model.error.NoDeliveryFoundException;
import ru.yandex.practicum.model.order.OrderDto;
import ru.yandex.practicum.model.warehous.AddressDto;
import ru.yandex.practicum.model.warehous.ShippedToDeliveryRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DeliveryServiceImpl implements DeliveryService {

    private final OrderFeignClient orderFeignClient;
    private final DeliveryRepository deliveryRepository;
    private final WarehouseFeignClient warehouseFeignClient;

    @Override
    public DeliveryDto createDelivery(final DeliveryDto deliveryDto) {
        final Delivery delivery = Delivery.builder()
                .addressFrom(DeliveryMapper.fromAddressDto(deliveryDto.getFromAddress()))
                .addressTo(DeliveryMapper.fromAddressDto(deliveryDto.getToAddress()))
                .order(deliveryDto.getOrderId())
                .state(deliveryDto.getDeliveryState())
                .build();
        deliveryRepository.save(delivery);
        return deliveryDto;
    }

    @Override
    public void successfulDelivery(final String deliveryId) {
        final Delivery delivery = deliveryRepository
                .findByDeliveryId(deliveryId)
                .orElseThrow(() -> new NoDeliveryFoundException(
                        "Доставка не найдена.",
                        "Доставка с id " + deliveryId + " не найдена.",
                        HttpStatus.NOT_FOUND
                ));
        delivery.setState(DeliveryState.DELIVERED);
        deliveryRepository.save(delivery);

        orderFeignClient.deliveryOrder(deliveryId);

        warehouseFeignClient.shippedProductsInDelivery(
                ShippedToDeliveryRequest
                        .builder()
                        .deliveryId(deliveryId)
                        .orderId(delivery.getOrder())
                        .build()
        );
    }

    @Override
    public void pickedDelivery(final String deliveryId) {
        final Delivery delivery = deliveryRepository
                .findByDeliveryId(deliveryId)
                .orElseThrow(() -> new NoDeliveryFoundException(
                        "Доставка не найдена.",
                        "Доставка с id " + deliveryId + " не найдена.",
                        HttpStatus.NOT_FOUND
                ));
        delivery.setState(DeliveryState.IN_PROGRESS);
        deliveryRepository.save(delivery);
    }

    @Override
    public void failedDelivery(final String deliveryId) {
        final Delivery delivery = deliveryRepository
                .findByDeliveryId(deliveryId)
                .orElseThrow(() -> new NoDeliveryFoundException(
                        "Доставка не найдена.",
                        "Доставка с id " + deliveryId + " не найдена.",
                        HttpStatus.NOT_FOUND
                ));
        delivery.setState(DeliveryState.FAILED);
        deliveryRepository.save(delivery);

        orderFeignClient.deliverFailedOrder(deliveryId);
    }

    @Override
    public BigDecimal cost(final OrderDto orderDto) {
        final Delivery delivery = deliveryRepository
                .findByDeliveryId(orderDto.getDeliveryId())
                .orElseThrow(() -> new NoDeliveryFoundException(
                        "Доставка не найдена.",
                        "Доставка с id " + orderDto.getDeliveryId() + " не найдена.",
                        HttpStatus.NOT_FOUND
                ));

        delivery.setFragile(orderDto.isFragile());
        delivery.setTotalVolume(orderDto.getDeliveryVolume());
        delivery.setTotalWeight(orderDto.getDeliveryWeight());

        final AddressDto warehouseAddressDto = warehouseFeignClient.getAddress();
        final String warehouseAddressName = warehouseAddressDto != null ?
                warehouseAddressDto.getCity() : "ADDRESS_1";

        final int warehouseMultiplier = extractWarehouseMultiplier(warehouseAddressName);

        final String clientAddress = delivery.getAddressTo() != null ?
                delivery.getAddressTo().getStreet() : "";

        BigDecimal totalCost = calculateDeliveryCost(
                warehouseMultiplier,
                delivery.getTotalWeight(),
                delivery.getTotalVolume(),
                delivery.getFragile(),
                warehouseAddressName,
                clientAddress
        );

        deliveryRepository.save(delivery);

        return totalCost.setScale(2, RoundingMode.HALF_UP);
    }

    private int extractWarehouseMultiplier(final String warehouseAddress) {
        if (warehouseAddress == null || warehouseAddress.isEmpty()) {
            return 1;
        }

        String addressUpper = warehouseAddress.toUpperCase();

        String digits = addressUpper.replaceAll("\\D+", "");
        if (!digits.isEmpty()) {
            try {
                return Integer.parseInt(digits);
            } catch (NumberFormatException e) {
                return 1;
            }
        }

        return 1;
    }

    private boolean isSameStreet(final String warehouseAddress, final String clientAddress) {
        if (warehouseAddress == null || clientAddress == null) {
            return false;
        }

        String warehouseStreet = extractStreetName(warehouseAddress);
        String clientStreet = extractStreetName(clientAddress);

        return warehouseStreet.equalsIgnoreCase(clientStreet);
    }

    private String extractStreetName(final String address) {
        if (address == null || address.isEmpty()) {
            return "";
        }

        String cleaned = address.split(",")[0].trim();

        cleaned = cleaned.replaceAll("^\\d+\\s*", "");

        return cleaned.trim();
    }

    private BigDecimal calculateDeliveryCost(
            final int warehouseMultiplier,
            final BigDecimal weight,
            final BigDecimal volume,
            final Boolean fragile,
            final String warehouseAddress,
            final String clientAddress) {

        BigDecimal total = DeliveryDefault.BASE_COST;

        BigDecimal warehouseCost = DeliveryDefault.BASE_COST.multiply(
                BigDecimal.valueOf(warehouseMultiplier)
        );
        total = total.add(warehouseCost);

        if (fragile != null && fragile) {
            BigDecimal fragileCost = total.multiply(DeliveryDefault.FRAGILE_MULTIPLIER);
            total = total.add(fragileCost);
        }

        if (weight != null) {
            BigDecimal weightCost = weight.multiply(DeliveryDefault.WEIGHT_MULTIPLIER);
            total = total.add(weightCost);
        }

        if (volume != null) {
            BigDecimal volumeCost = volume.multiply(DeliveryDefault.VOLUME_MULTIPLIER);
            total = total.add(volumeCost);
        }

        if (!isSameStreet(warehouseAddress, clientAddress)) {
            BigDecimal addressSurcharge = total.multiply(DeliveryDefault.ADDRESS_SURCHARGE_MULTIPLIER);
            total = total.add(addressSurcharge);
        }

        return total;
    }
}
