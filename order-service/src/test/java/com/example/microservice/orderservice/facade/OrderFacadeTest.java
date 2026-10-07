package com.example.microservice.orderservice.facade;

import com.example.microservice.dto.ApiResponse;
import com.example.microservice.dto.CartItemResponse;
import com.example.microservice.dto.CartResponse;
import com.example.microservice.dto.OrderResponse;
import com.example.microservice.dto.ProductResponse;
import com.example.microservice.exception.BusinessException;
import com.example.microservice.model.Order;
import com.example.microservice.model.OrderItem;
import com.example.microservice.orderservice.client.CartClient;
import com.example.microservice.orderservice.client.ProductClient;
import com.example.microservice.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderFacadeTest {

    private final OrderService orderService = mock(OrderService.class);
    private final CartClient cartClient = mock(CartClient.class);
    private final ProductClient productClient = mock(ProductClient.class);
    private final OrderFacade orderFacade = new OrderFacade(orderService, cartClient, productClient);

    @Test
    void createOrderDecrementsStockAndClearsCart() {
        CartResponse cart = cartResponse(List.of(new CartItemResponse(1L, 10L, 2)));
        ProductResponse product = productResponse(10L, "Shoe", "10.00", 100);
        Order saved = new Order();
        saved.setId(1L);

        when(cartClient.getCart(1L)).thenReturn(ApiResponse.success(cart));
        when(productClient.getProduct(10L)).thenReturn(ApiResponse.success(product));
        when(productClient.decreaseStock(10L, 2)).thenReturn(ApiResponse.success(null));
        when(cartClient.clearCart(1L)).thenReturn(ApiResponse.success(null));
        when(orderService.createOrder(eq(1L), anySet(), any(BigDecimal.class))).thenReturn(saved);

        OrderResponse result = orderFacade.createOrderFromCart(1L);

        assertThat(result.id()).isEqualTo(1L);
        verify(productClient).decreaseStock(10L, 2);
        verify(cartClient).clearCart(1L);
    }

    @Test
    void createOrderRejectsEmptyCart() {
        CartResponse cart = cartResponse(List.of());
        when(cartClient.getCart(1L)).thenReturn(ApiResponse.success(cart));

        assertThatThrownBy(() -> orderFacade.createOrderFromCart(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cart is empty");
        verify(orderService, never()).createOrder(any(), anySet(), any());
    }

    @Test
    void createOrderPropagatesInsufficientStock() {
        CartResponse cart = cartResponse(List.of(new CartItemResponse(1L, 10L, 5)));
        ProductResponse product = productResponse(10L, "Shoe", "10.00", 2);

        when(cartClient.getCart(1L)).thenReturn(ApiResponse.success(cart));
        when(productClient.getProduct(10L)).thenReturn(ApiResponse.success(product));
        when(productClient.decreaseStock(10L, 5))
                .thenReturn(ApiResponse.error(com.example.microservice.dto.ApiStatus.BAD_REQUEST,
                        "Insufficient stock for product: Shoe"));

        assertThatThrownBy(() -> orderFacade.createOrderFromCart(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");
        verify(orderService, never()).createOrder(any(), anySet(), any());
        verify(cartClient, never()).clearCart(any());
    }

    @Test
    void cancelOrderRestoresStock() {
        Order order = new Order();
        order.setId(5L);
        order.setStatus("PENDING");
        order.setItems(new HashSet<>(Set.of(orderItem(10L, 3))));
        when(orderService.getOrderById(5L)).thenReturn(Optional.of(order));
        when(productClient.increaseStock(10L, 3)).thenReturn(ApiResponse.success(null));
        when(orderService.markCancelled(5L)).thenReturn(true);

        orderFacade.cancelOrder(5L);

        verify(orderService).markCancelled(5L);
        verify(productClient).increaseStock(10L, 3);
    }

    @Test
    void cancelOrderRejectsWhenConcurrentlyCancelled() {
        Order order = new Order();
        order.setId(5L);
        order.setStatus("PENDING");
        order.setItems(new HashSet<>(Set.of(orderItem(10L, 3))));
        when(orderService.getOrderById(5L)).thenReturn(Optional.of(order));
        when(orderService.markCancelled(5L)).thenReturn(false);

        assertThatThrownBy(() -> orderFacade.cancelOrder(5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pending");
        verify(productClient, never()).increaseStock(any(), anyInt());
    }

    @Test
    void cancelOrderRejectsNonPending() {
        Order order = new Order();
        order.setId(5L);
        order.setStatus("SHIPPED");
        order.setItems(new HashSet<>());
        when(orderService.getOrderById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderFacade.cancelOrder(5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pending");
        verify(productClient, never()).increaseStock(any(), anyInt());
    }

    @Test
    void createOrderCompensatesStockWhenClearCartFails() {
        CartResponse cart = cartResponse(List.of(new CartItemResponse(1L, 10L, 2)));
        ProductResponse product = productResponse(10L, "Shoe", "10.00", 100);
        when(cartClient.getCart(1L)).thenReturn(ApiResponse.success(cart));
        when(productClient.getProduct(10L)).thenReturn(ApiResponse.success(product));
        when(productClient.decreaseStock(10L, 2)).thenReturn(ApiResponse.success(null));
        when(orderService.createOrder(eq(1L), anySet(), any(BigDecimal.class))).thenReturn(new Order());
        when(cartClient.clearCart(1L))
                .thenReturn(ApiResponse.error(com.example.microservice.dto.ApiStatus.INTERNAL_ERROR, "cart down"));

        assertThatThrownBy(() -> orderFacade.createOrderFromCart(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cart down");
        verify(productClient).increaseStock(10L, 2);
    }

    @Test
    void createOrderCompensatesEarlierItemsWhenLaterDecreaseFails() {
        CartResponse cart = cartResponse(List.of(new CartItemResponse(1L, 10L, 2), new CartItemResponse(2L, 11L, 1)));
        ProductResponse first = productResponse(10L, "Shoe", "10.00", 100);
        ProductResponse second = productResponse(11L, "Socks", "5.00", 0);
        when(cartClient.getCart(1L)).thenReturn(ApiResponse.success(cart));
        when(productClient.getProduct(10L)).thenReturn(ApiResponse.success(first));
        when(productClient.getProduct(11L)).thenReturn(ApiResponse.success(second));
        when(productClient.decreaseStock(10L, 2)).thenReturn(ApiResponse.success(null));
        when(productClient.decreaseStock(11L, 1))
                .thenReturn(ApiResponse.error(com.example.microservice.dto.ApiStatus.BAD_REQUEST,
                        "Insufficient stock for product: Socks"));

        assertThatThrownBy(() -> orderFacade.createOrderFromCart(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");
        verify(productClient).increaseStock(10L, 2);
        verify(productClient, never()).increaseStock(11L, 1);
    }

    @Test
    void cancelOrderCompensatesWhenRestockFails() {
        Order order = new Order();
        order.setId(5L);
        order.setStatus("PENDING");
        order.setItems(new HashSet<>(Set.of(orderItem(10L, 3), orderItem(11L, 1))));
        when(orderService.getOrderById(5L)).thenReturn(Optional.of(order));
        when(orderService.markCancelled(5L)).thenReturn(true);
        when(productClient.increaseStock(10L, 3)).thenReturn(ApiResponse.success(null));
        when(productClient.increaseStock(11L, 1))
                .thenReturn(ApiResponse.error(com.example.microservice.dto.ApiStatus.INTERNAL_ERROR, "product down"));

        assertThatThrownBy(() -> orderFacade.cancelOrder(5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("restore stock");
        verify(productClient).decreaseStock(10L, 3);
    }

    private CartResponse cartResponse(List<CartItemResponse> items) {
        return new CartResponse(1L, 1L, LocalDateTime.now(), items);
    }

    private ProductResponse productResponse(Long id, String name, String price, int stock) {
        return new ProductResponse(id, name, "desc", new BigDecimal(price), stock,
                null, 1L, LocalDateTime.now());
    }

    private OrderItem orderItem(Long productId, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }
}
