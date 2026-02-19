package com.paymenthub.router.service;

import com.paymenthub.common.dto.RabbitMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RouterService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-keys.to-ms2}")
    private String toMs2RoutingKey;

    @Value("${rabbitmq.routing-keys.to-ms3}")
    private String toMs3RoutingKey;

    // ─────────────────────────────────────────────────────────────
    // Listens from MS1
    // Reads destination field → forwards to correct MS
    // No payload inspection. No business logic.
    // ─────────────────────────────────────────────────────────────
    @RabbitListener(
        queues = "${rabbitmq.queues.from-ms1}",
        concurrency = "10-50"
    )
    public void route(RabbitMessage message) {
        long start = System.currentTimeMillis();

        String destination = message.getDestination();
        String correlationId = message.getCorrelationId();

        log.info("🔀 Router received | correlationId={} | destination={}",
                correlationId, destination);

        // Decide where to send based on destination header
        String routingKey = resolveRoutingKey(destination);

        // Forward the SAME message unchanged
        rabbitTemplate.convertAndSend(exchange, routingKey, message);

        log.info("✅ Routed in {}ms | {} → {}",
                System.currentTimeMillis() - start,
                correlationId,
                routingKey);
    }

    // ─────────────────────────────────────────────────────────────
    // Routing rules:
    //   NPCI       → MS2
    //   RUPAY      → MS2
    //   VISA       → MS3
    //   MASTERCARD → MS3
    //   default    → MS2
    // ─────────────────────────────────────────────────────────────
    private String resolveRoutingKey(String destination) {
        if (destination == null || destination.isBlank()) {
            log.warn("⚠️ No destination set, defaulting to MS2");
            return toMs2RoutingKey;
        }

        return switch (destination.toUpperCase()) {
            case "NPCI",
                 "RUPAY"      -> toMs2RoutingKey;
            case "VISA",
                 "MASTERCARD" -> toMs3RoutingKey;
            default           -> {
                log.warn("⚠️ Unknown destination '{}', defaulting to MS2",
                        destination);
                yield toMs2RoutingKey;
            }
        };
    }
}