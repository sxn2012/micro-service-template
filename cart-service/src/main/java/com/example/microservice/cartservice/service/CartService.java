package com.example.microservice.cartservice.service;

import com.example.microservice.dto.AddCartItemRequest;
import com.example.microservice.exception.BusinessException;
import com.example.microservice.model.Cart;
import com.example.microservice.model.CartItem;
import com.example.microservice.cartservice.mapper.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;

    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUserId(userId);
                    cart.setCreatedAt(LocalDateTime.now());
                    return cartRepository.save(cart);
                });
    }

    public Cart addItem(Cart cart, AddCartItemRequest request) {
        cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.productId()))
                .findFirst()
                .ifPresentOrElse(
                        item -> item.setQuantity(item.getQuantity() + request.quantity()),
                        () -> {
                            CartItem newItem = new CartItem();
                            newItem.setProductId(request.productId());
                            newItem.setQuantity(request.quantity());
                            cart.getItems().add(newItem);
                        }
                );
        return cartRepository.save(cart);
    }

    public Cart updateItemQuantity(Cart cart, Long itemId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new BusinessException("数量至少为 1");
        }
        cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .ifPresent(item -> item.setQuantity(quantity));
        return cartRepository.save(cart);
    }

    public Cart removeItem(Cart cart, Long itemId) {
        cart.getItems().removeIf(item -> item.getId().equals(itemId));
        return cartRepository.save(cart);
    }

    public void clearCart(Cart cart) {
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    public void deleteByUserId(Long userId) {
        cartRepository.findByUserId(userId).ifPresent(cartRepository::delete);
    }
}
