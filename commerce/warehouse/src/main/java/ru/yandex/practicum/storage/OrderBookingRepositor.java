package ru.yandex.practicum.storage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.yandex.practicum.model.warehous.OrderBooking;

public interface OrderBookingRepositor extends JpaRepository<OrderBooking, Long> {

    @Query("SELECT ob " +
            "FROM OrderBooking  ob " +
            "WHERE ob.orderId = :orderId")
    OrderBooking findOrderBookingByOrderBookingId(String orderId);
}
