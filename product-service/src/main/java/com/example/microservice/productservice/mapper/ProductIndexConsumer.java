package com.example.microservice.productservice.mapper;

import com.example.microservice.message.ProductIndexEvent;
import com.example.microservice.message.RabbitTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductIndexConsumer {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 200;

    private final ProductRepository productRepository;
    private final ProductSearchMapper productSearchMapper;

    @RabbitListener(queues = RabbitTopics.PRODUCT_INDEX_QUEUE)
    public void onProductIndex(ProductIndexEvent event) {
        try {
            syncWithRetry(event);
        } catch (Exception e) {
            log.error("Failed to sync product index for event {} (bootstrap will reindex at next restart)", event, e);
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }

    private void syncWithRetry(ProductIndexEvent event) {
        int attempts = 0;
        while (true) {
            try {
                if (event.action() == ProductIndexEvent.IndexAction.DELETE) {
                    productSearchMapper.delete(event.productId());
                } else {
                    productRepository.findById(event.productId()).ifPresent(productSearchMapper::index);
                }
                return;
            } catch (Exception e) {
                if (++attempts >= MAX_ATTEMPTS) {
                    throw e;
                }
                log.warn("Product index sync attempt {} failed for event {}, retrying...", attempts, event);
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }
}
