package com.ecommerce.order.dto;

import lombok.Data;

@Data
public class ProductDto {
    private Long id;
    private String name;
    private Double price;
    private Integer stockQuantity;
}
