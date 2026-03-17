package de.marvinrichter.stranglerfig.newservice.adapter.out.persistence;

import de.marvinrichter.stranglerfig.newservice.domain.Order;
import de.marvinrichter.stranglerfig.newservice.domain.OrderStatus;
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

    OrderJpaEntity(UUID id, String customerId, BigDecimal totalAmount, OrderStatus status) {
        this.id = id;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    Order toDomain() {
        return new Order(id, customerId, totalAmount, status);
    }
}
