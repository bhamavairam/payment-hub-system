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

    @Value("${rabbitmq.queues.to-ms2}")
    private String toMs2Queue;

    @Value("${rabbitmq.queues.from-ms2}")
    private String fromMs2Queue;

    @Value("${rabbitmq.queues.dead-letter}")
    private String deadLetterQueue;

    @Value("${rabbitmq.routing-keys.to-ms2}")
    private String toMs2RoutingKey;

    @Value("${rabbitmq.routing-keys.from-ms2}")
    private String fromMs2RoutingKey;

    // ─── EXCHANGE ───────────────────────────────────────────
    // One exchange handles ALL payment messages
    @Bean
    public DirectExchange paymentExchange() {
        return ExchangeBuilder
                .directExchange(exchange)
                .durable(true)  // Survives RabbitMQ restart
                .build();
    }

    // ─── QUEUES ─────────────────────────────────────────────

    // Queue: MS1 → MS2 (send plain JSON to MS2)
    @Bean
    public Queue toMs2Queue() {
        return QueueBuilder
                .durable(toMs2Queue)
                .withArgument("x-dead-letter-exchange", exchange)
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .withArgument("x-message-ttl", 30000)  // 30s TTL
                .build();
    }

    // Queue: MS2 → MS1 (receive response from MS2)
    @Bean
    public Queue fromMs2Queue() {
        return QueueBuilder
                .durable(fromMs2Queue)
                .withArgument("x-dead-letter-exchange", exchange)
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                .withArgument("x-message-ttl", 30000)
                .build();
    }

    // Queue: Dead letter (failed messages)
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
                .durable(deadLetterQueue)
                .build();
    }

    // ─── BINDINGS ────────────────────────────────────────────

    // Bind MS2 request queue to exchange
    @Bean
    public Binding toMs2Binding() {
        return BindingBuilder
                .bind(toMs2Queue())
                .to(paymentExchange())
                .with(toMs2RoutingKey);
    }

    // Bind MS1 response queue to exchange
    @Bean
    public Binding fromMs2Binding() {
        return BindingBuilder
                .bind(fromMs2Queue())
                .to(paymentExchange())
                .with(fromMs2RoutingKey);
    }

    // Bind dead letter queue
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(paymentExchange())
                .with("dead.letter");
    }

    // ─── MESSAGE CONVERTER ───────────────────────────────────
    // Convert Java objects to JSON automatically
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(mapper);
    }

    // ─── RABBIT TEMPLATE ─────────────────────────────────────
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}