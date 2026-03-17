package de.marvinrichter.stranglerfig.newservice.adapter.out.persistence;

import de.marvinrichter.stranglerfig.newservice.application.OrderPersistencePort;
import de.marvinrichter.stranglerfig.newservice.domain.Order;
import org.springframework.stereotype.Repository;

@Repository
public class JpaOrderAdapter implements OrderPersistencePort {

    private final NewOrderJpaRepository jpaRepository;

    public JpaOrderAdapter(NewOrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Order save(Order order) {
        var entity = new OrderJpaEntity(
                order.id(), order.customerId(), order.totalAmount(), order.status());
        jpaRepository.save(entity);
        return order;
    }
}
