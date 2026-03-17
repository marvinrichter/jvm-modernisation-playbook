package de.marvinrichter.bba.after.application;

import de.marvinrichter.bba.after.domain.Order;

/**
 * The port interface — Step 1 of Branch-by-Abstraction.
 *
 * <p>All callers (controller, use cases, schedulers) depend on this interface.
 * The concrete implementation is swapped via {@code @ConditionalOnProperty}
 * without any caller knowing.
 */
public interface OrderPort {
    Order createOrder(CreateOrderCommand command);
}
