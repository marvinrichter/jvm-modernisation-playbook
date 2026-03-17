package de.marvinrichter.stranglerfig.legacy;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

/**
 * The legacy order controller — active by default (feature flag = false).
 *
 * <p>When {@code feature.new-order-service.enabled=false} (the default), this bean
 * is registered and handles {@code POST /api/orders}.
 *
 * <p>Classic problems this demonstrates:
 * <ul>
 *   <li>Direct dependency on JPA repository (no port abstraction)</li>
 *   <li>Business logic ("status = PENDING") mixed with persistence</li>
 *   <li>Returns an untyped Map — fragile for consumers</li>
 * </ul>
 *
 * <p>Strangler Fig step: once the new service is ready, flip the flag to
 * deactivate this bean and activate {@code NewOrderController}.
 */
@RestController
@RequestMapping("/api/orders")
@ConditionalOnProperty(name = "feature.new-order-service.enabled",
        havingValue = "false", matchIfMissing = true)
public class LegacyOrderController {

    private final OrderJpaRepository orderJpaRepository;

    public LegacyOrderController(OrderJpaRepository orderJpaRepository) {
        this.orderJpaRepository = orderJpaRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestBody Map<String, Object> body) {

        var entity = new OrderEntity(
                (String) body.get("customerId"),
                new BigDecimal(body.get("totalAmount").toString()));

        orderJpaRepository.save(entity);

        return ResponseEntity.ok(Map.of(
                "orderId", entity.getId(),
                "status",  entity.getStatus(),
                "source",  "legacy"));
    }
}
