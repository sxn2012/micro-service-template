package com.example.microservice.orderservice.config;

import com.example.microservice.message.MqEvent;
import com.example.microservice.message.RabbitTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqEventForwarder {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 200;

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void forward(MqEvent event) {
        try {
            sendWithRetry(event);
        } catch (Exception e) {
            log.error("Send MQ event routingKey={} failed after {} attempts, giving up",
                    event.routingKey(), MAX_ATTEMPTS, e);
        }
    }

    private void sendWithRetry(MqEvent event) {
        int attempts = 0;
        while (true) {
            try {
                rabbitTemplate.convertAndSend(RabbitTopics.EXCHANGE, event.routingKey(), event.payload());
                return;
            } catch (Exception e) {
                if (++attempts >= MAX_ATTEMPTS) {
                    throw e;
                }
                log.warn("Send MQ event routingKey={} failed (attempt {}), retrying...", event.routingKey(), attempts, e);
                sleep();
            }
        }
    }

    private void sleep() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while retrying MQ send", e);
        }
    }
}
