package com.example.microservice.message;

public record MqEvent(String routingKey, Object payload) {
}
