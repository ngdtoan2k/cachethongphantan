package com.ecommerce.order.service;

import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;

import java.util.List;

// interface định nghĩa các phương thức của service để implement trong orderserciceimpl
//controller sẽ gọi đến đây để thực hiện nghiệp vụ.
public interface OrderService {
    OrderResponse createOrder(OrderRequest request);

    OrderResponse getOrderById(Long id);

    List<OrderResponse> getAllOrders();

    List<OrderResponse> getOrdersByUserId(Long userId);
}
