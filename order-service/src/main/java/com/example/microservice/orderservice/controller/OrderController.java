package com.example.microservice.orderservice.controller;

import com.example.microservice.dto.ApiResponse;
import com.example.microservice.dto.OrderResponse;
import com.example.microservice.orderservice.facade.OrderFacade;
import com.example.microservice.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderFacade orderFacade;

    @PostMapping("/from-cart")
    public ApiResponse<OrderResponse> createOrderFromCart() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success("Order created", orderFacade.createOrderFromCart(userId));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> getMyOrders() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(orderFacade.getOrdersByUserId(userId));
    }

    @GetMapping("/all")
    public ApiResponse<Page<OrderResponse>> getAllOrders(Pageable pageable) {
        SecurityUtils.requireAdmin();
        return ApiResponse.success(orderFacade.getAllOrders(pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrderById(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return orderFacade.getOrderById(id)
                .filter(order -> order.userId().equals(userId))
                .map(order -> ApiResponse.<OrderResponse>success(order))
                .orElseThrow(() -> new com.example.microservice.exception.ResourceNotFoundException("Order not found: " + id));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        SecurityUtils.requireAdmin();
        return ApiResponse.success("Order status updated", orderFacade.updateOrderStatus(id, status));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancelOrder(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean admin = SecurityUtils.isAdmin();
        orderFacade.getOrderById(id)
                .filter(order -> admin || order.userId().equals(userId))
                .orElseThrow(() -> new com.example.microservice.exception.ResourceNotFoundException("Order not found: " + id));
        orderFacade.cancelOrder(id);
        return ApiResponse.success("Order cancelled", null);
    }

    @DeleteMapping("/user/{userId}")
    public ApiResponse<Void> deleteUserOrders(@PathVariable Long userId) {
        SecurityUtils.requireAdmin();
        orderFacade.deleteUserOrders(userId);
        return ApiResponse.success("Orders deleted", null);
    }
}
