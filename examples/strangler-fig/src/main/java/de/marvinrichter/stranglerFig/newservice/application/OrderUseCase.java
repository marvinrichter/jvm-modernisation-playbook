package de.marvinrichter.stranglerfig.newservice.application;

import de.marvinrichter.stranglerfig.newservice.domain.Order;
import de.marvinrichter.stranglerfig.newservice.domain.OrderStatus;
import org.springframework.stereotype.Service;

/**
 * Application service (use case) — orchestrates domain and ports.
 * No JPA, no HTTP — pure business logic.
 */
@Service
public class OrderUseCase {

    private final OrderPersistencePort orderPersistencePort;

    public OrderUseCase(OrderPersistencePort orderPersistencePort) {
        this.orderPersistencePort = orderPersistencePort;
    }

    public Order createOrder(CreateOrderCommand command) {
        var order = Order.create(command.customerId(), command.totalAmount());
        return orderPersistencePort.save(order);
    }
}
