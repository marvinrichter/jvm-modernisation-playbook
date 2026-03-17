package de.marvinrichter.bba.after.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record Order(UUID id, String customerId, BigDecimal totalAmount, OrderStatus status) {

    public static Order create(String customerId, BigDecimal totalAmount) {
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("totalAmount must be positive");
        }
        return new Order(UUID.randomUUID(), customerId, totalAmount, OrderStatus.PENDING);
    }
}
