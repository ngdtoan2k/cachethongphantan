package com.ecommerce.cart.messaging;

import com.ecommerce.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Lắng nghe sự kiện OrderCreatedEvent để xóa giỏ hàng sau khi đặt hàng thành công.
 *
 * Cơ chế xử lý lỗi:
 * - Nếu clearCart() thất bại → throw exception → RabbitMQ sẽ tự retry (tối đa 3 lần, cách 2s)
 * - Sau 3 lần thất bại → message chuyển vào Dead Letter Queue (cart-clear.queue.dlq)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final CartService cartService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("[cart-service] Received OrderCreatedEvent - orderId={}, userId={}",
                event.getOrderId(), event.getUserId());
        try {
            cartService.clearCart(event.getUserId());
            log.info("[cart-service] Cart cleared successfully for userId={}", event.getUserId());
        } catch (Exception e) {
            log.error("[cart-service] Failed to clear cart for userId={}. Error: {}. Will retry...",
                    event.getUserId(), e.getMessage());
            // Throw để trigger retry mechanism của RabbitMQ
            throw new RuntimeException("Failed to clear cart for userId=" + event.getUserId(), e);
        }
    }
}
