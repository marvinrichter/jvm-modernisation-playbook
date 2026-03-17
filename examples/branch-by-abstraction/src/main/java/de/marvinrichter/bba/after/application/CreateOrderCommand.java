package de.marvinrichter.bba.after.application;

import java.math.BigDecimal;
import java.util.Objects;

public record CreateOrderCommand(String customerId, BigDecimal totalAmount) {
    public CreateOrderCommand {
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(totalAmount, "totalAmount must not be null");
    }
}
