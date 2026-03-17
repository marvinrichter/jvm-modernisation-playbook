package de.marvinrichter.stranglerfig.newservice.adapter.in.web;

import de.marvinrichter.stranglerfig.newservice.application.OrderUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * New hexagonal inbound adapter — active when the feature flag is enabled.
 *
 * <p>When {@code feature.new-order-service.enabled=true}, this bean is registered
 * and handles {@code POST /api/orders} instead of {@code LegacyOrderController}.
 *
 * <p>The URL contract ({@code /api/orders}) is preserved — clients don't notice the switch.
 * The response gains a typed structure and a "source=new-service" indicator for monitoring.
 */
@RestController
@RequestMapping("/api/orders")
@ConditionalOnProperty(name = "feature.new-order-service.enabled", havingValue = "true")
public class NewOrderController {

    private final OrderUseCase orderUseCase;

    public NewOrderController(OrderUseCase orderUseCase) {
        this.orderUseCase = orderUseCase;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        var order = orderUseCase.createOrder(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }
}
