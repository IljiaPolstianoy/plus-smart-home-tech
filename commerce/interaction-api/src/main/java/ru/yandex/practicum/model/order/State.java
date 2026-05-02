package ru.yandex.practicum.model.order;

import java.util.Set;

public enum State {
    NEW,
    ON_PAYMENT,
    ON_DELIVERY,
    DONE,
    DELIVERED,
    ASSEMBLED,
    PAID,
    COMPLETED,
    DELIVERY_FAILED,
    ASSEMBLY_FAILED,
    PAYMENT_FAILED,
    PRODUCT_RETURNED,
    CANCELED;

    public boolean canTransitionTo(State newState) {
        return getAllowedTransitions().contains(newState);
    }

    private Set<State> getAllowedTransitions() {
        return switch (this) {
            case NEW -> Set.of(ASSEMBLED, ASSEMBLY_FAILED, ON_PAYMENT, ON_DELIVERY);
            case ASSEMBLED -> Set.of(PAID, PAYMENT_FAILED);
            case PAID -> Set.of(DELIVERED, DELIVERY_FAILED);
            case DELIVERED -> Set.of(COMPLETED);
            case ASSEMBLY_FAILED, PAYMENT_FAILED -> Set.of(CANCELED);
            case DELIVERY_FAILED -> Set.of(PRODUCT_RETURNED);
            default -> Set.of();
        };
    }
}
