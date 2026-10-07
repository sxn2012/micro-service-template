package com.example.microservice.productservice.config;

import com.example.microservice.message.RabbitTopics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(RabbitTopics.EXCHANGE, true, false);
    }

    @Bean
    public Queue productIndexQueue() {
        return new Queue(RabbitTopics.PRODUCT_INDEX_QUEUE, true);
    }

    @Bean
    public Queue stockEventQueue() {
        return new Queue(RabbitTopics.STOCK_EVENT_QUEUE, true);
    }

    @Bean
    public Binding productIndexBinding(TopicExchange eventExchange, Queue productIndexQueue) {
        return BindingBuilder.bind(productIndexQueue).to(eventExchange).with("product.*");
    }

    @Bean
    public Binding stockEventBinding(TopicExchange eventExchange, Queue stockEventQueue) {
        return BindingBuilder.bind(stockEventQueue).to(eventExchange).with("stock.*");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(mapper);
    }
}
