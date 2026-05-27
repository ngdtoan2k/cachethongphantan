package com.ecommerce.order.service;

import com.ecommerce.order.dto.CartItemDto;
import com.ecommerce.order.dto.ProductDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalServiceClient {

  private final RestTemplate restTemplate;

  @Value("${services.cart.url}")
  private String cartServiceUrl;

  @Value("${services.product.url}")
  private String productServiceUrl;

  @CircuitBreaker(name = "cart-service", fallbackMethod = "getCartFallback")
  public List<CartItemDto> getCartItems(Long userId) {
    String getCartUrl = cartServiceUrl + "/user/" + userId;

    log.info("Fetching cart for userId={} from {}", userId, getCartUrl);

    ResponseEntity<List<CartItemDto>> response = restTemplate.exchange(
        getCartUrl,
        HttpMethod.GET,
        null,
        new ParameterizedTypeReference<List<CartItemDto>>() {
        });

    return response.getBody();
  }

  public List<CartItemDto> getCartFallback(Long userId, Throwable ex) {
    log.error("Circuit Breaker [cart-service] activated for userId={}. Cause: {}",
        userId,
        ex == null ? "null" : ex.getMessage());

    throw new RuntimeException(
        "Cart service is unavailable. Please try again later.",
        ex);
  }

  @CircuitBreaker(name = "product-service", fallbackMethod = "getProductFallback")
  public ProductDto getProductWithCircuitBreaker(Long productId) {
    String getProductUrl = productServiceUrl + "/" + productId;

    log.info("Fetching product id={} from {}", productId, getProductUrl);

    return restTemplate.getForObject(getProductUrl, ProductDto.class);
  }

  public ProductDto getProductFallback(Long productId, Throwable ex) {
    log.error("Circuit Breaker [product-service] activated for productId={}. Cause: {}",
        productId,
        ex == null ? "null" : ex.getMessage());

    return null;
  }
}
