package ru.yandex.practicum.shopping;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@Embeddable
public class ShoppingCartAndProductId implements Serializable {
    private static final long serialVersionUID = -294360280371193845L;
    @Size(max = 36)
    @NotNull
    @Column(name = "shopping_cart_id", nullable = false, length = 36)
    private String shoppingCartId;

    @NotNull
    @Column(name = "product_id", nullable = false, length = Integer.MAX_VALUE)
    private String productId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        ShoppingCartAndProductId entity = (ShoppingCartAndProductId) o;
        return Objects.equals(this.productId, entity.productId) &&
                Objects.equals(this.shoppingCartId, entity.shoppingCartId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, shoppingCartId);
    }

}