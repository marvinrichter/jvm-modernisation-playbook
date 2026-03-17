package de.marvinrichter.bba.after.adapter.out.persistence;

import de.marvinrichter.bba.after.application.CreateOrderCommand;
import de.marvinrichter.bba.after.application.OrderPort;
import de.marvinrichter.bba.after.domain.Order;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

/**
 * The new hexagonal implementation of {@link OrderPort}.
 *
 * <p>Active when {@code feature.new-order-service=true}.
 * Once this has proven stable in production, delete {@code LegacyOrderAdapter}
 * and {@code LegacyOrderService} and remove the {@code @ConditionalOnProperty}.
 */
@Repository
@ConditionalOnProperty(name = "feature.new-order-service", havingValue = "true")
public class JpaOrderAdapter implements OrderPort {

    private final OrderJpaRepository jpaRepository;

    public JpaOrderAdapter(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Order createOrder(CreateOrderCommand command) {
        var order = Order.create(command.customerId(), command.totalAmount());
        jpaRepository.save(new OrderJpaEntity(order));
        return order;
    }
}
