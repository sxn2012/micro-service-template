package com.example.microservice.orderservice.facade;

import com.example.microservice.dto.ApiResponse;
import com.example.microservice.dto.CartItemResponse;
import com.example.microservice.dto.CartResponse;
import com.example.microservice.dto.OrderResponse;
import com.example.microservice.dto.ProductResponse;
import com.example.microservice.exception.BusinessException;
import com.example.microservice.exception.ResourceNotFoundException;
import com.example.microservice.model.Order;
import com.example.microservice.model.OrderItem;
import com.example.microservice.orderservice.client.CartClient;
import com.example.microservice.orderservice.client.ProductClient;
import com.example.microservice.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;
    private final CartClient cartClient;
    private final ProductClient productClient;

    @Transactional
    public OrderResponse createOrderFromCart(Long userId) {
        ApiResponse<CartResponse> cartResp = cartClient.getCart(userId);
        if (cartResp.code() != 200 || cartResp.data() == null) {
            throw new BusinessException(cartResp.message());
        }
        CartResponse cart = cartResp.data();

        if (cart.items().isEmpty()) {
            throw new BusinessException("Cart is empty");
        }

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> prepared = new ArrayList<>();

        for (CartItemResponse cartItem : cart.items()) {
            ApiResponse<ProductResponse> productResp = productClient.getProduct(cartItem.productId());
            if (productResp.code() != 200 || productResp.data() == null) {
                throw new ResourceNotFoundException("Product not found: " + cartItem.productId());
            }
            ProductResponse product = productResp.data();

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.id());
            orderItem.setProductName(product.name());
            orderItem.setQuantity(cartItem.quantity());
            orderItem.setPrice(product.price());
            prepared.add(orderItem);

            total = total.add(product.price().multiply(BigDecimal.valueOf(cartItem.quantity())));
        }

        Set<OrderItem> orderItems = new HashSet<>(prepared);
        List<OrderItem> decreased = new ArrayList<>();
        try {
            for (OrderItem item : prepared) {
                ApiResponse<Void> stockResp = productClient.decreaseStock(item.getProductId(), item.getQuantity());
                if (stockResp.code() != 200) {
                    throw new BusinessException("Insufficient stock for product: " + item.getProductName());
                }
                decreased.add(item);
            }

            Order savedOrder = orderService.createOrder(userId, orderItems, total);

            ApiResponse<Void> clearResp = cartClient.clearCart(userId);
            if (clearResp.code() != 200) {
                throw new BusinessException(clearResp.message() == null ? "Failed to clear cart" : clearResp.message());
            }
            return OrderResponse.from(savedOrder);
        } catch (RuntimeException e) {
            restoreStockQuietly(decreased);
            throw e;
        }
    }

    private void restoreStockQuietly(List<OrderItem> items) {
        for (OrderItem item : items) {
            try {
                productClient.increaseStock(item.getProductId(), item.getQuantity());
            } catch (RuntimeException e) {
                log.error("Stock compensation failed for product {} quantity {} — manual stock fix required",
                        item.getProductId(), item.getQuantity(), e);
            }
        }
    }

    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderService.getAllOrders(pageable).map(OrderResponse::from);
    }

    public Optional<OrderResponse> getOrderById(Long id) {
        return orderService.getOrderById(id).map(OrderResponse::from);
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderService.getOrdersByUserId(userId).stream().map(OrderResponse::from).toList();
    }

    public OrderResponse updateOrderStatus(Long id, String status) {
        return OrderResponse.from(orderService.updateOrderStatus(id, status));
    }

    public void deleteUserOrders(Long userId) {
        orderService.deleteByUserId(userId);
    }

    @Transactional
    public void cancelOrder(Long id) {
        Order order = orderService.getOrderById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        if (!"PENDING".equals(order.getStatus())) {
            throw new BusinessException("Only pending orders can be cancelled");
        }
        if (!orderService.markCancelled(id)) {
            throw new BusinessException("Only pending orders can be cancelled");
        }
        List<OrderItem> restored = new ArrayList<>();
        try {
            for (OrderItem item : order.getItems()) {
                ApiResponse<Void> resp = productClient.increaseStock(item.getProductId(), item.getQuantity());
                if (resp.code() != 200) {
                    throw new BusinessException("Failed to restore stock for product: " + item.getProductName());
                }
                restored.add(item);
            }
        } catch (RuntimeException e) {
            for (OrderItem item : restored) {
                try {
                    productClient.decreaseStock(item.getProductId(), item.getQuantity());
                } catch (RuntimeException ex) {
                    log.error("Stock compensation failed for product {} quantity {} — manual stock fix required",
                            item.getProductId(), item.getQuantity(), ex);
                }
            }
            throw e;
        }
    }
}
