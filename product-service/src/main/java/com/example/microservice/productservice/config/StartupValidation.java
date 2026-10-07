package com.example.microservice.productservice.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class StartupValidation {

    private final RedisConnectionFactory redisConnectionFactory;
    private final ElasticsearchClient elasticsearchClient;
    private final ConnectionFactory rabbitConnectionFactory;

    @EventListener(ApplicationReadyEvent.class)
    public void validateExternalServices() {
        try {
            redisConnectionFactory.getConnection().ping();
        } catch (Exception e) {
            throw new RuntimeException("Redis不可用，应用启动失败", e);
        }

        try {
            elasticsearchClient.ping();
        } catch (IOException e) {
            throw new RuntimeException("Elasticsearch不可用，应用启动失败", e);
        }

        try (org.springframework.amqp.rabbit.connection.Connection connection =
                     rabbitConnectionFactory.createConnection()) {
            if (!connection.isOpen()) {
                throw new IllegalStateException("connection closed");
            }
        } catch (Exception e) {
            throw new RuntimeException("RabbitMQ不可用，应用启动失败", e);
        }
    }
}
