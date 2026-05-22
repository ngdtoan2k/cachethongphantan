package com.ecommerce.cart.service;

import com.ecommerce.cart.dto.CartRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;

    @Override
    public CartResponse addToCart(CartRequest request) {
        Optional<CartItem> existingItem = cartRepository.findByUserIdAndProductId(
                request.getUserId(), request.getProductId()
        );

        CartItem item;
        if (existingItem.isPresent()) {
            item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
        } else {
            item = CartItem.builder()
                    .userId(request.getUserId())
                    .productId(request.getProductId())
                    .quantity(request.getQuantity())
                    .build();
        }

        CartItem savedItem = cartRepository.save(item);
        return mapToResponse(savedItem);
    }

    @Override
    public List<CartResponse> getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        cartRepository.deleteByUserId(userId);
    }

    private CartResponse mapToResponse(CartItem item) {
        return CartResponse.builder()
                .id(item.getId())
                .userId(item.getUserId())
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .build();
    }
}
