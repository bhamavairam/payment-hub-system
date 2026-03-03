package com.paymenthub.router.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    // Input queue
    @Value("${rabbitmq.queues.router-input}")
    private String routerInputQueue;

    // Output queues
    @Value("${rabbitmq.queues.transform-npci}")
    private String transformNpciQueue;

    @Value("${rabbitmq.queues.transform-visa}")
    private String transformVisaQueue;

    @Value("${rabbitmq.queues.transform-mastercard}")
    private String transformMastercardQueue;

    @Value("${rabbitmq.queues.fraud}")
    private String fraudQueue;

    @Value("${rabbitmq.queues.notification}")
    private String notificationQueue;

    // Routing keys
    @Value("${rabbitmq.routing-keys.transform-npci}")
    private String transformNpciKey;

    @Value("${rabbitmq.routing-keys.transform-visa}")
    private String transformVisaKey;

    @Value("${rabbitmq.routing-keys.transform-mastercard}")
    private String transformMastercardKey;

    @Value("${rabbitmq.routing-keys.fraud}")
    private String fraudKey;

    @Value("${rabbitmq.routing-keys.notification}")
    private String notificationKey;

    // ═══════════════════════════════════════════════════════
    // EXCHANGE
    // ═══════════════════════════════════════════════════════
    @Bean
    public DirectExchange paymentExchange() {
        return ExchangeBuilder
                .directExchange(exchange)
                .durable(true)
                .build();
    }

    // ═══════════════════════════════════════════════════════
    // QUEUES - INPUT
    // ═══════════════════════════════════════════════════════
    @Bean
    public Queue routerInputQueue() {
        return QueueBuilder
                .durable(routerInputQueue)
                .build();
    }

    // ═══════════════════════════════════════════════════════
    // QUEUES - OUTPUT (Transform queues)
    // ═══════════════════════════════════════════════════════
    @Bean
    public Queue transformNpciQueue() {
        return QueueBuilder
                .durable(transformNpciQueue)
                .build();
    }

    @Bean
    public Queue transformVisaQueue() {
        return QueueBuilder
                .durable(transformVisaQueue)
                .build();
    }

    @Bean
    public Queue transformMastercardQueue() {
        return QueueBuilder
                .durable(transformMastercardQueue)
                .build();
    }

    @Bean
    public Queue fraudQueue() {
        return QueueBuilder
                .durable(fraudQueue)
                .build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder
                .durable(notificationQueue)
                .build();
    }

    // ═══════════════════════════════════════════════════════
    // BINDINGS - OUTPUT QUEUES
    // ═══════════════════════════════════════════════════════
    @Bean
    public Binding transformNpciBinding() {
        return BindingBuilder
                .bind(transformNpciQueue())
                .to(paymentExchange())
                .with(transformNpciKey);
    }

    @Bean
    public Binding transformVisaBinding() {
        return BindingBuilder
                .bind(transformVisaQueue())
                .to(paymentExchange())
                .with(transformVisaKey);
    }

    @Bean
    public Binding transformMastercardBinding() {
        return BindingBuilder
                .bind(transformMastercardQueue())
                .to(paymentExchange())
                .with(transformMastercardKey);
    }

    @Bean
    public Binding fraudBinding() {
        return BindingBuilder
                .bind(fraudQueue())
                .to(paymentExchange())
                .with(fraudKey);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder
                .bind(notificationQueue())
                .to(paymentExchange())
                .with(notificationKey);
    }

    // ═══════════════════════════════════════════════════════
    // MESSAGE CONVERTER
    // ═══════════════════════════════════════════════════════
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    // ═══════════════════════════════════════════════════════
    // RABBIT TEMPLATE
    // ═══════════════════════════════════════════════════════
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}