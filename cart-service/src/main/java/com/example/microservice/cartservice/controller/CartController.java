package com.example.microservice.cartservice.controller;

import com.example.microservice.cartservice.facade.CartFacade;
import com.example.microservice.cartservice.service.CartService;
import com.example.microservice.dto.AddCartItemRequest;
import com.example.microservice.dto.ApiResponse;
import com.example.microservice.dto.CartResponse;
import com.example.microservice.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartFacade cartFacade;
    private final CartService cartService;

    @GetMapping("/{userId}")
    public ApiResponse<CartResponse> getCart(@PathVariable Long userId) {
        SecurityUtils.requireOwnerOrAdmin(userId);
        return ApiResponse.success(cartFacade.getCartByUserId(userId));
    }

    @PostMapping("/{userId}/items")
    public ApiResponse<CartResponse> addItem(@PathVariable Long userId, @Valid @RequestBody AddCartItemRequest request) {
        SecurityUtils.requireOwnerOrAdmin(userId);
        return ApiResponse.success("Item added to cart", cartFacade.addItem(userId, request));
    }

    @PutMapping("/{userId}/items/{itemId}")
    public ApiResponse<CartResponse> updateItemQuantity(@PathVariable Long userId, @PathVariable Long itemId,
                                                        @RequestParam Integer quantity) {
        SecurityUtils.requireOwnerOrAdmin(userId);
        return ApiResponse.success("Cart item updated", cartFacade.updateItemQuantity(userId, itemId, quantity));
    }

    @DeleteMapping("/{userId}/items/{itemId}")
    public ApiResponse<CartResponse> removeItem(@PathVariable Long userId, @PathVariable Long itemId) {
        SecurityUtils.requireOwnerOrAdmin(userId);
        return ApiResponse.success("Item removed from cart", cartFacade.removeItem(userId, itemId));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> clearCart(@PathVariable Long userId) {
        SecurityUtils.requireOwnerOrAdmin(userId);
        cartFacade.clearCart(userId);
        return ApiResponse.success("Cart cleared", null);
    }

    @DeleteMapping("/user/{userId}")
    public ApiResponse<Void> deleteUserCart(@PathVariable Long userId) {
        SecurityUtils.requireAdmin();
        cartService.deleteByUserId(userId);
        return ApiResponse.success("Cart deleted", null);
    }
}
