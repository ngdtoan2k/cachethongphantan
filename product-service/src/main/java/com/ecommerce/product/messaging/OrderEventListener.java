package com.ecommerce.product.messaging;

import com.ecommerce.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final ProductService productService;
    private static final int MAX_RETRIES = 3;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent to update inventory for order {}", event.getOrderId());
        if (event.getItems() != null) {
            for (OrderCreatedEvent.OrderItemDto item : event.getItems()) {
                updateStockWithRetry(item.getProductId(), item.getQuantity(), 1);
            }
        }
    }

    private void updateStockWithRetry(Long productId, Integer quantity, int attempt) {
        try {
            productService.updateStock(productId, quantity);
            log.info("Stock updated successfully for product {} (quantity: {})", productId, quantity);
        } catch (Exception e) {
            log.warn("Failed to update stock for product {} (attempt {}/{}). Error: {}",
                    productId, attempt, MAX_RETRIES, e.getMessage());

            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(500 * attempt); // Exponential backoff
                    updateStockWithRetry(productId, quantity, attempt + 1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Retry interrupted for product {}", productId);
                }
            } else {
                log.error("Failed to update stock for product {} after {} attempts. Manual intervention needed.",
                        productId, MAX_RETRIES, e);
            }
        }
    }
}
