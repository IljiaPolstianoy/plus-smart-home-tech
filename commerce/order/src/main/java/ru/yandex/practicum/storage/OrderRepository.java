package ru.yandex.practicum.storage;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.model.order.OrderDto;

import javax.swing.text.html.Option;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<OrderDto, Long> {

    @Query("SELECT or " +
            "FROM OrderDto or " +
            "WHERE or.shoppingCartId IN (" +
            "SELECT sc.shoppingCartId " +
            "FROM ShoppingCart sc " +
            "WHERE sc.userName = :userName" +
            ") " +
            "ORDER BY or.orderId DESC "
    )
    Page<OrderDto> findAllByUserName(@Param("userName") String userName, Pageable pageable);

    Optional<OrderDto> findByOrderId(@Param("orderId") String orderId);
}
