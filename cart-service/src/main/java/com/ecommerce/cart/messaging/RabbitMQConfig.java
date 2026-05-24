package com.ecommerce.cart.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Cấu hình RabbitMQ cho cart-service với:
 * - Retry tự động khi xử lý message thất bại (3 lần, mỗi lần cách 2 giây)
 * - Dead Letter Queue (DLQ): message quá 3 lần thất bại → vào cart-clear.queue.dlq
 */
@Configuration
public class RabbitMQConfig {

    public static final String QUEUE          = "cart-clear.queue";
    public static final String DLQ            = "cart-clear.queue.dlq";
    public static final String EXCHANGE       = "ecommerce.exchange";
    public static final String DLQ_EXCHANGE   = "ecommerce.exchange.dlq";
    public static final String ROUTING_KEY    = "order.created";

    // ───── Main Queue (khai báo DLQ) ─────

    @Bean
    public Queue queue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DLQ_EXCHANGE);
        args.put("x-dead-letter-routing-key", DLQ);
        return QueueBuilder.durable(QUEUE).withArguments(args).build();
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    // ───── Dead Letter Queue ─────

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLQ_EXCHANGE);
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DLQ);
    }

    // ───── Converter ─────

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ───── Retry: 3 lần, mỗi lần chờ 2 giây ─────

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(2000); // chờ 2 giây giữa các lần retry

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3); // tối đa 3 lần thử

        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setRetryPolicy(retryPolicy);
        return retryTemplate;
    }

    /**
     * Sau khi hết retry → republish message sang DLQ để không mất dữ liệu.
     */
    @Bean
    public MessageRecoverer messageRecoverer(
            org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, DLQ_EXCHANGE, DLQ);
    }

    /**
     * Listener factory tích hợp retry + DLQ recoverer.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageRecoverer messageRecoverer) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setPrefetchCount(1);
        // Gắn retry + recoverer vào listener
        org.springframework.amqp.rabbit.config.RetryInterceptorBuilder.StatefulRetryInterceptorBuilder builder =
                org.springframework.amqp.rabbit.config.RetryInterceptorBuilder.stateful();
        builder.maxAttempts(3)
               .backOffOptions(2000, 1.0, 2000)
               .recoverer(messageRecoverer);
        factory.setAdviceChain(builder.build());
        return factory;
    }
}
