package com.ecommerce.order.dto;

import lombok.Data;

@Data
public class CartItemDto {
    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;
}
