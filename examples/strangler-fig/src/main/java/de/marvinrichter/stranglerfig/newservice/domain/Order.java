package de.marvinrichter.stranglerfig.newservice.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Pure domain object — no framework dependencies, no annotations.
 */
public record Order(
        UUID id,
        String customerId,
        BigDecimal totalAmount,
        OrderStatus status
) {
    public static Order create(String customerId, BigDecimal totalAmount) {
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be positive");
        }
        return new Order(UUID.randomUUID(), customerId, totalAmount, OrderStatus.PENDING);
    }
}
