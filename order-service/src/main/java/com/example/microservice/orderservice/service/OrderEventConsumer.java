package com.example.microservice.orderservice.service;

import com.example.microservice.message.OrderEventMessage;
import com.example.microservice.message.RabbitTopics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventConsumer {

    @RabbitListener(queues = RabbitTopics.ORDER_EVENT_QUEUE)
    public void onOrderEvent(OrderEventMessage event) {
        log.info("[MQ][order] eventType={} orderId={} userId={} status={} totalAmount={}",
                event.eventType(), event.orderId(), event.userId(), event.status(), event.totalAmount());
    }
}
