package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.CartRequest;
import com.ecommerce.cart.dto.CartResponse;

import java.util.List;

public interface CartService {
    CartResponse addToCart(CartRequest request);
    List<CartResponse> getCartByUserId(Long userId);
    void clearCart(Long userId);
}
