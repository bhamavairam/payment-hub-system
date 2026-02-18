package com.paymenthub.ms2.service;

import com.paymenthub.ms2.dto.SarvatraEncryptedRequest;
import com.paymenthub.ms2.dto.SarvatraResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
@Slf4j
public class SarvatraCommunicationService {

    @Autowired
    private WebClient sarvatraWebClient;

    @Value("${sarvatra.switch.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${sarvatra.switch.retry.delay:2000}")
    private long retryDelay;

    public Mono<SarvatraResponse> sendToSarvatra(String correlationId, SarvatraEncryptedRequest request) {
        log.info("Sending to Sarvatra Switch - Correlation ID: {}", correlationId);

        return sarvatraWebClient
                .post()
                .uri("/process")  // Adjust endpoint as per Sarvatra documentation
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("X-Correlation-ID", correlationId)
                .header("X-Request-ID", java.util.UUID.randomUUID().toString())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SarvatraResponse.class)
                .retryWhen(Retry.backoff(maxRetryAttempts, Duration.ofMillis(retryDelay))
                        .filter(throwable -> throwable instanceof WebClientResponseException.ServiceUnavailable 
                                || throwable instanceof java.net.ConnectException)
                        .doBeforeRetry(retrySignal -> 
                                log.warn("Retrying request to Sarvatra - Attempt: {}, Correlation ID: {}", 
                                        retrySignal.totalRetries() + 1, correlationId)))
                .doOnSuccess(response -> 
                        log.info("Received response from Sarvatra - Correlation ID: {}, Response Code: {}", 
                                correlationId, response.getResponseCode()))
                .doOnError(error -> 
                        log.error("Error from Sarvatra - Correlation ID: {}, Error: {}", 
                                correlationId, error.getMessage()))
                .onErrorResume(error -> {
                    log.error("Failed to communicate with Sarvatra after retries - Correlation ID: {}", 
                            correlationId, error);
                    return Mono.just(SarvatraResponse.builder()
                            .responseCode("ERROR")
                            .responseMessage("Communication failed: " + error.getMessage())
                            .build());
                });
    }

    public Mono<SarvatraResponse> sendToSarvatraSync(String correlationId, SarvatraEncryptedRequest request) {
        return sendToSarvatra(correlationId, request)
                .timeout(Duration.ofSeconds(35))
                .onErrorResume(error -> {
                    log.error("Timeout or error sending to Sarvatra - Correlation ID: {}", correlationId, error);
                    return Mono.just(SarvatraResponse.builder()
                            .responseCode("TIMEOUT")
                            .responseMessage("Request timeout")
                            .build());
                });
    }
}