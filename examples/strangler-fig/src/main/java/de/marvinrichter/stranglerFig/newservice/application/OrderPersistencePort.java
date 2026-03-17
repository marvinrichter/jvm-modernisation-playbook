package de.marvinrichter.stranglerfig.newservice.application;

import de.marvinrichter.stranglerfig.newservice.domain.Order;

/**
 * Outbound port — defines what the application needs from persistence.
 * The JPA adapter implements this; the application layer never knows about JPA.
 */
public interface OrderPersistencePort {
    Order save(Order order);
}
