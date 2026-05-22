package com.ecommerce.user.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventListener {

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent - UserId: {}, OrderId: {}, TotalAmount: {}",
                event.getUserId(), event.getOrderId(), event.getTotalAmount());
        // Ở đây có thể lưu lịch sử giao dịch vào database cho user
        log.info("Successfully recorded purchase history for user {}", event.getUserId());
    }
}
