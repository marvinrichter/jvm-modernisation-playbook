package de.marvinrichter.bba.after.adapter.out.persistence;

import de.marvinrichter.bba.after.domain.Order;
import de.marvinrichter.bba.after.domain.OrderStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "new_orders")
class OrderJpaEntity {

    @Id
    private UUID id;
    private String customerId;
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    protected OrderJpaEntity() {}

    OrderJpaEntity(Order order) {
        this.id = order.id();
        this.customerId = order.customerId();
        this.totalAmount = order.totalAmount();
        this.status = order.status();
    }

    Order toDomain() {
        return new Order(id, customerId, totalAmount, status);
    }
}
