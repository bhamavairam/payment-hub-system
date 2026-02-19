package com.paymenthub.router.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
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

    @Value("${rabbitmq.queues.from-ms1}")
    private String fromMs1Queue;

    @Value("${rabbitmq.queues.to-ms2}")
    private String toMs2Queue;

    @Value("${rabbitmq.queues.to-ms3}")
    private String toMs3Queue;

    @Value("${rabbitmq.routing-keys.from-ms1}")
    private String fromMs1RoutingKey;

    @Value("${rabbitmq.routing-keys.to-ms2}")
    private String toMs2RoutingKey;

    @Value("${rabbitmq.routing-keys.to-ms3}")
    private String toMs3RoutingKey;

    // ── Exchange ──────────────────────────────────────────────────
    @Bean
    public DirectExchange paymentExchange() {
        return ExchangeBuilder
                .directExchange(exchange)
                .durable(true)
                .build();
    }

    // ── Queues ────────────────────────────────────────────────────

    // Queue where MS1 publishes (router reads from here)
    @Bean
    public Queue fromMs1Queue() {
        return QueueBuilder.durable(fromMs1Queue).build();
    }

    // Queue for MS2 (NPCI / RuPay)
    @Bean
    public Queue toMs2Queue() {
        return QueueBuilder.durable(toMs2Queue).build();
    }

    // Queue for MS3 (VISA / Mastercard)
    @Bean
    public Queue toMs3Queue() {
        return QueueBuilder.durable(toMs3Queue).build();
    }

    // ── Bindings ──────────────────────────────────────────────────

    @Bean
    public Binding fromMs1Binding() {
        return BindingBuilder
                .bind(fromMs1Queue())
                .to(paymentExchange())
                .with(fromMs1RoutingKey);
    }

    @Bean
    public Binding toMs2Binding() {
        return BindingBuilder
                .bind(toMs2Queue())
                .to(paymentExchange())
                .with(toMs2RoutingKey);
    }

    @Bean
    public Binding toMs3Binding() {
        return BindingBuilder
                .bind(toMs3Queue())
                .to(paymentExchange())
                .with(toMs3RoutingKey);
    }

    // ── Message Converter ─────────────────────────────────────────
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    // ── Rabbit Template ───────────────────────────────────────────
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    // ── Listener Factory ──────────────────────────────────────────
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }
}