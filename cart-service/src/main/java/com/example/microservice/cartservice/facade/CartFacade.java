package com.example.microservice.cartservice.facade;

import com.example.microservice.cartservice.client.ProductClient;
import com.example.microservice.cartservice.service.CartService;
import com.example.microservice.dto.AddCartItemRequest;
import com.example.microservice.dto.ApiResponse;
import com.example.microservice.dto.CartResponse;
import com.example.microservice.dto.ProductResponse;
import com.example.microservice.exception.BusinessException;
import com.example.microservice.exception.ResourceNotFoundException;
import com.example.microservice.model.Cart;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartFacade {

    private final CartService cartService;
    private final ProductClient productClient;

    public CartResponse getCartByUserId(Long userId) {
        return CartResponse.from(cartService.getCartByUserId(userId));
    }

    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        ApiResponse<ProductResponse> productResp = productClient.getProduct(request.productId());
        if (productResp.code() != 200) {
            throw new BusinessException(productResp.message());
        }
        ProductResponse product = productResp.data();
        if (product == null) {
            throw new ResourceNotFoundException("Product not found: " + request.productId());
        }
        if (product.stock() < request.quantity()) {
            throw new BusinessException("Insufficient stock for product: " + product.name());
        }

        Cart cart = cartService.getCartByUserId(userId);
        return CartResponse.from(cartService.addItem(cart, request));
    }

    public CartResponse updateItemQuantity(Long userId, Long itemId, Integer quantity) {
        Cart cart = cartService.getCartByUserId(userId);
        return CartResponse.from(cartService.updateItemQuantity(cart, itemId, quantity));
    }

    public CartResponse removeItem(Long userId, Long itemId) {
        Cart cart = cartService.getCartByUserId(userId);
        return CartResponse.from(cartService.removeItem(cart, itemId));
    }

    public void clearCart(Long userId) {
        Cart cart = cartService.getCartByUserId(userId);
        cartService.clearCart(cart);
    }
}
