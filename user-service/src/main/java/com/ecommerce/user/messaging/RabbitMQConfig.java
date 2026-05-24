package com.ecommerce.user.messaging;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Cấu hình RabbitMQ cho user-service với:
 * - Retry tự động khi xử lý message thất bại (3 lần, mỗi lần cách 2 giây)
 * - Dead Letter Queue (DLQ): message quá 3 lần thất bại → vào user-history.queue.dlq
 */
@Configuration
public class RabbitMQConfig {

    public static final String QUEUE        = "user-history.queue";
    public static final String DLQ          = "user-history.queue.dlq";
    public static final String EXCHANGE     = "ecommerce.exchange";
    public static final String DLQ_EXCHANGE = "ecommerce.exchange.dlq";
    public static final String ROUTING_KEY  = "order.created";

    // ───── Main Queue (có khai báo DLQ) ─────

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

    // ───── Converter & Template ─────

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         ObservationRegistry observationRegistry) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        // Bật observation để traceId được ghi vào headers khi publish DLQ
        template.setObservationEnabled(true);
        return template;
    }

    /**
     * Sau khi hết retry → republish message sang DLQ để không mất dữ liệu.
     */
    @Bean
    public MessageRecoverer messageRecoverer(RabbitTemplate rabbitTemplate) {
        return new RepublishMessageRecoverer(rabbitTemplate, DLQ_EXCHANGE, DLQ);
    }

    /**
     * Listener factory tích hợp retry (3 lần, delay 2 giây) + DLQ recoverer.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageRecoverer messageRecoverer,
            ObservationRegistry observationRegistry) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
        factory.setPrefetchCount(1);
        // Bật observation để extract traceId từ AMQP headers → inject vào MDC của consumer thread
        factory.setObservationEnabled(true);
        factory.setAdviceChain(
            RetryInterceptorBuilder.stateful()
                .maxAttempts(3)
                .backOffOptions(2000, 1.0, 2000) // delay 2 giây, không tăng dần
                .recoverer(messageRecoverer)
                .build()
        );
        return factory;
    }
}
