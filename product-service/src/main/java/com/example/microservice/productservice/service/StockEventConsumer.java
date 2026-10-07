package com.example.microservice.productservice.service;

import com.example.microservice.message.RabbitTopics;
import com.example.microservice.message.StockEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StockEventConsumer {

    @RabbitListener(queues = RabbitTopics.STOCK_EVENT_QUEUE)
    public void onStockEvent(StockEventMessage event) {
        log.info("[MQ][stock] eventType={} productId={} quantity={}",
                event.eventType(), event.productId(), event.quantity());
    }
}
