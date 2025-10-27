package com.venueconnect.order;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/pay")
    public ResponseEntity<OrderResponse> createOrderAndPay(
            @RequestBody PaymentRequest request
    ) {
        OrderResponse orderResponse = orderService.createOrder(request);
        return ResponseEntity.ok(orderResponse);
    }
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        OrderResponse cancelledOrder = orderService.cancelOrder(id);
        return ResponseEntity.ok(cancelledOrder);
    }
}