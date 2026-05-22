package com.ecommerce.order.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    public void publishOrderCreatedEvent(OrderCreatedEvent event) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);
                log.info("OrderCreatedEvent published successfully for orderId: {} (attempt {})",
                        event.getOrderId(), attempt);
                return;
            } catch (Exception e) {
                log.warn("Failed to publish OrderCreatedEvent for orderId: {} (attempt {}/{}). Error: {}",
                        event.getOrderId(), attempt, MAX_RETRIES, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry interrupted for orderId: {}", event.getOrderId());
                        break;
                    }
                } else {
                    log.error(
                            "Failed to publish OrderCreatedEvent for orderId: {} after {} attempts. Order created but messaging failed.",
                            event.getOrderId(), MAX_RETRIES, e);
                }
            }
        }
    }
}
