package com.ecommerce.order.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Lắng nghe các sự kiện chuyển trạng thái của Resilience4j Circuit Breaker
 * và ghi log khi circuit breaker mở hoặc thay đổi trạng thái.
 */
@Configuration
@Slf4j
public class CircuitBreakerEventConfig {

  @EventListener
  public void onStateTransition(CircuitBreakerOnStateTransitionEvent event) {
    CircuitBreaker.State fromState = event.getStateTransition().getFromState();
    CircuitBreaker.State toState = event.getStateTransition().getToState();

    log.info("Circuit breaker '{}' state changed: {} -> {}",
        event.getCircuitBreakerName(),
        fromState,
        toState);

    if (toState == CircuitBreaker.State.OPEN) {
      log.warn("Circuit breaker '{}' is now OPEN. Calls will be rejected until it moves to HALF_OPEN.",
          event.getCircuitBreakerName());
    }
  }
}
