package de.marvinrichter.stranglerfig.newservice.adapter.in.web;

import de.marvinrichter.stranglerfig.newservice.application.CreateOrderCommand;

import java.math.BigDecimal;

public record CreateOrderRequest(String customerId, BigDecimal totalAmount) {
    public CreateOrderCommand toCommand() {
        return new CreateOrderCommand(customerId, totalAmount);
    }
}
