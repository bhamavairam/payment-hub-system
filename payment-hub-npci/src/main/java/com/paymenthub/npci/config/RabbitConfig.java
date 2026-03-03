package com.paymenthub.npci.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    // ─── Exchange ────────────────────────────────────────────────────────────

    @Bean
    public DirectExchange paymentHubExchange() {
        return new DirectExchange(exchange, true, false);
    }

    // ─── Queues ──────────────────────────────────────────────────────────────

    @Bean
    public Queue npciQueue() {
        return QueueBuilder.durable("transform.npci.queue").build();
    }

    @Bean
    public Queue fraudResponseQueue() {
        return QueueBuilder.durable("fraud.response.queue").build();
    }

    @Bean
    public Queue replyQueue() {
        return QueueBuilder.durable("ms1.reply.queue").build();
    }

    // ─── Converter ───────────────────────────────────────────────────────────

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }

    // ─── Bindings ────────────────────────────────────────────────────────────

    @Bean
    public Binding npciBinding(Queue npciQueue, DirectExchange paymentHubExchange) {
        return BindingBuilder
                .bind(npciQueue)
                .to(paymentHubExchange)
                .with("transform.npci");             // matches Router's routingKey
    }

    @Bean
    public Binding fraudBinding(Queue fraudResponseQueue, DirectExchange paymentHubExchange) {
        return BindingBuilder
                .bind(fraudResponseQueue)
                .to(paymentHubExchange)
                .with("fraud.check");                // matches Router's routingKey
    }

    @Bean
    public Binding replyBinding(Queue replyQueue, DirectExchange paymentHubExchange) {
        return BindingBuilder
                .bind(replyQueue)
                .to(paymentHubExchange)
                .with("ms1.response.key");           // matches ResponseCoordinatorService
    }
}