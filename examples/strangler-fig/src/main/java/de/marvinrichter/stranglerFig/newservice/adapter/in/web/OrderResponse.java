package de.marvinrichter.stranglerfig.newservice.adapter.in.web;

import de.marvinrichter.stranglerfig.newservice.domain.Order;
import de.marvinrichter.stranglerfig.newservice.domain.OrderStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponse(UUID orderId, String customerId, BigDecimal totalAmount,
                             OrderStatus status, String source) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.id(), order.customerId(), order.totalAmount(), order.status(), "new-service");
    }
}
