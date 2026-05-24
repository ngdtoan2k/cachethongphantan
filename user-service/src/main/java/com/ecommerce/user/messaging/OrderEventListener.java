package com.ecommerce.user.messaging;

import com.ecommerce.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Lắng nghe sự kiện OrderCreatedEvent để cập nhật số đơn hàng của user.
 *
 * Cơ chế xử lý lỗi:
 * - Nếu incrementOrderCount() thất bại → throw exception → RabbitMQ retry (tối đa 3 lần, cách 2s)
 * - Sau 3 lần thất bại → message chuyển vào Dead Letter Queue (user-history.queue.dlq)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener {

    private final UserService userService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("[user-service] Received OrderCreatedEvent - orderId={}, userId={}, totalAmount={}",
                event.getOrderId(), event.getUserId(), event.getTotalAmount());
        try {
            userService.incrementOrderCount(event.getUserId());
            log.info("[user-service] Successfully incremented totalOrders for userId={}",
                    event.getUserId());
        } catch (Exception e) {
            log.error("[user-service] Failed to increment totalOrders for userId={}. Error: {}. Will retry...",
                    event.getUserId(), e.getMessage());
            // Throw để trigger retry mechanism của RabbitMQ
            throw new RuntimeException("Failed to update order count for userId=" + event.getUserId(), e);
        }
    }
}
