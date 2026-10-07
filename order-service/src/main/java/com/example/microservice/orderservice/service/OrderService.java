package com.example.microservice.orderservice.service;

import com.example.microservice.exception.BusinessException;
import com.example.microservice.exception.ResourceNotFoundException;
import com.example.microservice.message.MqEvent;
import com.example.microservice.message.OrderEventMessage;
import com.example.microservice.message.RabbitTopics;
import com.example.microservice.model.Order;
import com.example.microservice.model.OrderItem;
import com.example.microservice.orderservice.mapper.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "PENDING", Set.of("PAID"),
            "PAID", Set.of("SHIPPED"),
            "SHIPPED", Set.of("DELIVERED"),
            "DELIVERED", Set.of(),
            "CANCELLED", Set.of());

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public Order createOrder(Long userId, Set<OrderItem> items, BigDecimal totalAmount) {
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus("PENDING");
        order.setTotalAmount(totalAmount);
        order.setCreatedAt(LocalDateTime.now());
        order.setItems(items);
        Order saved = orderRepository.save(order);
        eventPublisher.publishEvent(new MqEvent(RabbitTopics.ORDER_CREATED, OrderEventMessage.created(saved)));
        return saved;
    }

    public Page<Order> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order updateOrderStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        if ("CANCELLED".equals(status)) {
            throw new BusinessException("取消订单请使用 /orders/{id}/cancel 接口，以便回补库存");
        }
        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(order.getStatus(), Set.of());
        if (!ALLOWED_TRANSITIONS.containsKey(status)) {
            throw new BusinessException("非法订单状态: " + status);
        }
        if (!allowed.contains(status)) {
            throw new BusinessException("订单状态不允许从 " + order.getStatus() + " 变更为 " + status);
        }
        order.setStatus(status);
        Order saved = orderRepository.save(order);
        String routingKey = "CANCELLED".equals(status) ? RabbitTopics.ORDER_CANCELLED : RabbitTopics.ORDER_STATUS;
        eventPublisher.publishEvent(new MqEvent(routingKey, OrderEventMessage.statusChanged(saved)));
        return saved;
    }

    public void deleteByUserId(Long userId) {
        orderRepository.findByUserId(userId).forEach(orderRepository::delete);
    }

    public boolean markCancelled(Long id) {
        int updated = orderRepository.updateStatusIfMatches(id, "CANCELLED", "PENDING");
        if (updated == 0) {
            return false;
        }
        Order order = orderRepository.findById(id).orElse(null);
        if (order != null) {
            eventPublisher.publishEvent(new MqEvent(RabbitTopics.ORDER_CANCELLED,
                    OrderEventMessage.statusChanged(order)));
        }
        return true;
    }
}
