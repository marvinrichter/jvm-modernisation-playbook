package de.marvinrichter.bba.after.application;

import de.marvinrichter.bba.after.domain.Order;
import de.marvinrichter.bba.after.domain.OrderStatus;
import de.marvinrichter.bba.before.LegacyOrderService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Wraps the legacy service behind the {@link OrderPort} interface.
 *
 * <p>Active by default ({@code feature.new-order-service=false}).
 * This is the "transitional" step — behaviour is unchanged, but all callers
 * now depend on the interface, not the concrete legacy class.
 */
@Service
@ConditionalOnProperty(name = "feature.new-order-service", havingValue = "false",
        matchIfMissing = true)
public class LegacyOrderAdapter implements OrderPort {

    private final LegacyOrderService legacyOrderService;

    public LegacyOrderAdapter(LegacyOrderService legacyOrderService) {
        this.legacyOrderService = legacyOrderService;
    }

    @Override
    public Order createOrder(CreateOrderCommand command) {
        // Translate legacy Map result into new domain object
        var result = legacyOrderService.createOrder(
                command.customerId(), command.totalAmount());
        return new Order(
                (java.util.UUID) result.get("orderId"),
                command.customerId(),
                command.totalAmount(),
                OrderStatus.PENDING);
    }
}
