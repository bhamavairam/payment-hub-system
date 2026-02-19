package com.paymenthub.ms3.config;

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

    @Value("${rabbitmq.queues.from-router}")
    private String fromRouterQueue;

    @Value("${rabbitmq.queues.to-ms1}")
    private String toMs1Queue;

    @Value("${rabbitmq.routing-keys.from-router}")
    private String fromRouterRoutingKey;

    @Value("${rabbitmq.routing-keys.to-ms1}")
    private String toMs1RoutingKey;

    @Bean
    public DirectExchange paymentExchange() {
        return ExchangeBuilder
                .directExchange(exchange)
                .durable(true)
                .build();
    }

    @Bean
    public Queue fromRouterQueue() {
        return QueueBuilder.durable(fromRouterQueue).build();
    }

    @Bean
    public Queue toMs1Queue() {
        return QueueBuilder.durable(toMs1Queue).build();
    }

    @Bean
    public Binding fromRouterBinding() {
        return BindingBuilder
                .bind(fromRouterQueue())
                .to(paymentExchange())
                .with(fromRouterRoutingKey);
    }

    @Bean
    public Binding toMs1Binding() {
        return BindingBuilder
                .bind(toMs1Queue())
                .to(paymentExchange())
                .with(toMs1RoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

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