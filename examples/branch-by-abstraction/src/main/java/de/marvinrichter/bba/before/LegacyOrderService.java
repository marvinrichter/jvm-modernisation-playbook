package de.marvinrichter.bba.before;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The legacy service — this is what Branch-by-Abstraction replaces.
 *
 * <p>Problems:
 * <ul>
 *   <li>Mixed concerns: business logic + persistence in one class</li>
 *   <li>Business rule (amount > 0) is not enforced at the domain boundary</li>
 *   <li>Returns an untyped Map — callers must know field names as strings</li>
 *   <li>No interface — impossible to swap out without changing all callers</li>
 * </ul>
 */
@Service
public class LegacyOrderService {

    private final LegacyOrderJpaRepository orderJpaRepository;

    public LegacyOrderService(LegacyOrderJpaRepository orderJpaRepository) {
        this.orderJpaRepository = orderJpaRepository;
    }

    public Map<String, Object> createOrder(String customerId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        var entity = new LegacyOrderEntity(customerId, amount);
        orderJpaRepository.save(entity);
        return Map.of(
                "orderId", entity.getId(),
                "status",  entity.getStatus(),
                "source",  "legacy");
    }
}
