package com.ecommerce.order.service;

import com.ecommerce.order.dto.CartItemDto;
import com.ecommerce.order.dto.OrderRequest;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.ProductDto;
import com.ecommerce.order.entity.Order;
import com.ecommerce.order.entity.OrderItem;
import com.ecommerce.order.exception.ResourceNotFoundException;
import com.ecommerce.order.messaging.OrderCreatedEvent;
import com.ecommerce.order.messaging.OrderEventPublisher;
import com.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final RestTemplate restTemplate;
    private final OrderEventPublisher orderEventPublisher;

    @Value("${services.cart.url}")
    private String cartServiceUrl;

    @Value("${services.product.url}")
    private String productServiceUrl;

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        // 1. Fetch cart items for user
        String getCartUrl = cartServiceUrl + "/user/" + request.getUserId();
        ResponseEntity<List<CartItemDto>> cartResponse = restTemplate.exchange(
                getCartUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<CartItemDto>>() {}
        );

        List<CartItemDto> cartItems = cartResponse.getBody();
        if (cartItems == null || cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty for user: " + request.getUserId());
        }

        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setStatus("COMPLETED");

        List<OrderItem> orderItems = new ArrayList<>();
        double totalAmount = 0.0;

        List<OrderCreatedEvent.OrderItemDto> eventItems = new ArrayList<>();

        // 2. Fetch product details and calculate total amount
        for (CartItemDto cartItem : cartItems) {
            String getProductUrl = productServiceUrl + "/" + cartItem.getProductId();
            ProductDto product = restTemplate.getForObject(getProductUrl, ProductDto.class);

            if (product == null) {
                throw new ResourceNotFoundException("Product not found: " + cartItem.getProductId());
            }

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(cartItem.getProductId())
                    .quantity(cartItem.getQuantity())
                    .price(product.getPrice())
                    .build();
            
            orderItems.add(orderItem);
            totalAmount += product.getPrice() * cartItem.getQuantity();

            eventItems.add(new OrderCreatedEvent.OrderItemDto(product.getId(), cartItem.getQuantity()));
        }

        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);

        // 3. Save order to database
        Order savedOrder = orderRepository.save(order);

        // 4. Publish OrderCreatedEvent to RabbitMQ
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(savedOrder.getId())
                .userId(savedOrder.getUserId())
                .totalAmount(savedOrder.getTotalAmount())
                .items(eventItems)
                .build();
        
        orderEventPublisher.publishOrderCreatedEvent(event);

        return mapToResponse(savedOrder);
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        return mapToResponse(order);
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems().stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}
