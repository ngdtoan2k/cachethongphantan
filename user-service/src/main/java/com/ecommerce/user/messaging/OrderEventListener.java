package com.ecommerce.user.messaging;

import com.ecommerce.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener {

    private final UserService userService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent - UserId: {}, OrderId: {}, TotalAmount: {}",
                event.getUserId(), event.getOrderId(), event.getTotalAmount());
        try {
            userService.incrementOrderCount(event.getUserId());
            log.info("Successfully incremented totalOrders for userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("Failed to increment totalOrders for userId={}: {}", event.getUserId(), e.getMessage());
        }
    }
}
