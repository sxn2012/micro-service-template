package com.example.microservice.cartservice.mapper;

import com.example.microservice.model.Cart;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface CartRepository extends CrudRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);
}
