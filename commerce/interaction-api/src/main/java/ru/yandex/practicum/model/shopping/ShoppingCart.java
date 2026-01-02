package ru.yandex.practicum.model.shopping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "shopping_cart")
public class ShoppingCart {
    @Id
    @Size(max = 36)
    @Column(name = "shopping_cart_id", nullable = false, length = 36)
    private String shoppingCartId;

    @Column(name = "userName")
    private String userName;

    @Column(name = "shopping_cart_state")
    private ShoppingCartState shoppingCartState;
}