package ru.yandex.practicum.model.warehous;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class OrdersProductId implements Serializable {
    private static final long serialVersionUID = 1963492094149864683L;
    @NotNull
    @Column(name = "order_id", nullable = false, length = Integer.MAX_VALUE)
    private String orderId;

    @NotNull
    @Column(name = "product_id", nullable = false, length = Integer.MAX_VALUE)
    private String productId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        OrdersProductId entity = (OrdersProductId) o;
        return Objects.equals(this.productId, entity.productId) &&
                Objects.equals(this.orderId, entity.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, orderId);
    }

}