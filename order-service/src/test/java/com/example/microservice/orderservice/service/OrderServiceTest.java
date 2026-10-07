package com.example.microservice.orderservice.service;

import com.example.microservice.exception.BusinessException;
import com.example.microservice.exception.ResourceNotFoundException;
import com.example.microservice.message.MqEvent;
import com.example.microservice.message.OrderEventMessage;
import com.example.microservice.message.RabbitTopics;
import com.example.microservice.model.Order;
import com.example.microservice.model.OrderItem;
import com.example.microservice.orderservice.mapper.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final OrderService orderService = new OrderService(orderRepository, eventPublisher);

    @Test
    void createOrderPublishesCreatedEvent() {
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order created = orderService.createOrder(1L, Set.of(new OrderItem()), new BigDecimal("10.00"));

        assertThat(created.getStatus()).isEqualTo("PENDING");
        verify(eventPublisher).publishEvent(new MqEvent(RabbitTopics.ORDER_CREATED,
                OrderEventMessage.created(created)));
    }

    @Test
    void markCancelledFlipsPendingAndPublishesEvent() {
        Order cancelled = order(1L, "CANCELLED");
        when(orderRepository.updateStatusIfMatches(1L, "CANCELLED", "PENDING")).thenReturn(1);

        assertThat(orderService.markCancelled(1L)).isTrue();

        verify(eventPublisher).publishEvent(new MqEvent(RabbitTopics.ORDER_CANCELLED,
                OrderEventMessage.statusChanged(cancelled)));
    }

    @Test
    void markCancelledFailsWhenNotPending() {
        when(orderRepository.updateStatusIfMatches(1L, "CANCELLED", "PENDING")).thenReturn(0);

        assertThat(orderService.markCancelled(1L)).isFalse();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void pendingCanBecomePaid() {
        order(1L, "PENDING");
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order updated = orderService.updateOrderStatus(1L, "PAID");

        assertThat(updated.getStatus()).isEqualTo("PAID");
    }

    @Test
    void pendingCannotBeCancelledViaStatusUpdate() {
        order(1L, "PENDING");

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, "CANCELLED"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cancel");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void paidCanBecomeShippedAndShippedDelivered() {
        order(2L, "PAID");
        order(3L, "SHIPPED");
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(orderService.updateOrderStatus(2L, "SHIPPED").getStatus()).isEqualTo("SHIPPED");
        assertThat(orderService.updateOrderStatus(3L, "DELIVERED").getStatus()).isEqualTo("DELIVERED");
    }

    @Test
    void pendingCannotSkipToDelivered() {
        order(1L, "PENDING");
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, "DELIVERED"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void paidCannotRevertToPending() {
        order(2L, "PAID");
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> orderService.updateOrderStatus(2L, "PENDING"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许");
    }

    @Test
    void deliveredCannotChange() {
        order(4L, "DELIVERED");
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> orderService.updateOrderStatus(4L, "PAID"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不允许");
    }

    @Test
    void unknownStatusIsRejected() {
        order(1L, "PENDING");
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, "SHIPPING"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法订单状态");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updatingMissingOrderThrows() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(99L, "PAID"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Order order(Long id, String status) {
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        when(orderRepository.findById(id)).thenReturn(Optional.of(order));
        return order;
    }
}
