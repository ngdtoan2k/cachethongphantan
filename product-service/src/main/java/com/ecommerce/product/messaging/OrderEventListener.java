package com.ecommerce.product.messaging;

import com.ecommerce.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Lắng nghe OrderCreatedEvent và cập nhật inventory.
 * Nếu xử lý thất bại thì throw exception để RabbitMQ tự retry và sau 3 lần lỗi
 * sẽ đẩy vào DLQ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final ProductService productService;

    // "hãy gọi method này mỗi khi queue có message"
    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent to update inventory for order {}", event.getOrderId());
        if (event.getItems() != null) {
            for (OrderCreatedEvent.OrderItemDto item : event.getItems()) {
                try {
                    productService.updateStock(item.getProductId(), item.getQuantity());
                    log.info("Stock updated successfully for product {} (quantity: {})",
                            item.getProductId(), item.getQuantity());
                } catch (Exception e) {
                    log.error("Failed to update stock for product {}. Will retry via RabbitMQ. Error: {}",
                            item.getProductId(), e.getMessage(), e);
                    throw new RuntimeException("Failed to update stock for product " + item.getProductId(), e);
                }
            }
        }
    }
}
