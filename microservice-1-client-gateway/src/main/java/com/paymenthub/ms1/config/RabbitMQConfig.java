package com.paymenthub.ms1.config;

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

    @Value("${rabbitmq.queues.router}")
    private String routerQueue;

    @Value("${rabbitmq.queues.from-ms2}")
    private String responseQueue;

    @Value("${rabbitmq.routing-keys.router}")
    private String routerRoutingKey;

    @Value("${rabbitmq.routing-keys.response}")
    private String responseRoutingKey;

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
    // ROUTER QUEUE (MS1 → MS2)
    // ═══════════════════════════════════════════════════════
    @Bean
    public Queue routerQueue() {
        return QueueBuilder
                .durable(routerQueue)
                .withArgument("x-message-ttl", 30000)
                .build();
    }

    @Bean
    public Binding routerBinding() {
        return BindingBuilder
                .bind(routerQueue())
                .to(paymentExchange())
                .with(routerRoutingKey);
    }

    // ═══════════════════════════════════════════════════════
    // RESPONSE QUEUE (MS2 → MS1)  🔥 THIS WAS MISSING
    // ═══════════════════════════════════════════════════════
    @Bean
    public Queue responseQueue() {
        return QueueBuilder
                .durable(responseQueue)
                .build();
    }

    @Bean
    public Binding responseBinding() {
        return BindingBuilder
                .bind(responseQueue())
                .to(paymentExchange())
                .with(responseRoutingKey);
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