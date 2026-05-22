package com.ecommerce.cart.messaging;

import com.ecommerce.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final CartService cartService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent - Clearing cart for user {}", event.getUserId());
        try {
            cartService.clearCart(event.getUserId());
            log.info("Cart cleared successfully for user {}", event.getUserId());
        } catch (Exception e) {
            log.error("Error clearing cart for user {}: {}", event.getUserId(), e.getMessage());
        }
    }
}
