package de.marvinrichter.bba.after.adapter.in.web;

import de.marvinrichter.bba.after.application.CreateOrderCommand;
import de.marvinrichter.bba.after.application.OrderPort;
import de.marvinrichter.bba.after.domain.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * Inbound adapter — depends on {@link OrderPort}, not on any implementation.
 *
 * <p>This controller is identical regardless of whether the legacy adapter
 * or the new JPA adapter is active. The feature flag is invisible here.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderPort orderPort;

    public OrderController(OrderPort orderPort) {
        this.orderPort = orderPort;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        var order = orderPort.createOrder(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    record CreateOrderRequest(String customerId, BigDecimal totalAmount) {
        CreateOrderCommand toCommand() {
            return new CreateOrderCommand(customerId, totalAmount);
        }
    }

    record OrderResponse(String orderId, String customerId, BigDecimal totalAmount,
                         String status) {
        static OrderResponse from(Order order) {
            return new OrderResponse(
                    order.id().toString(),
                    order.customerId(),
                    order.totalAmount(),
                    order.status().name());
        }
    }
}
