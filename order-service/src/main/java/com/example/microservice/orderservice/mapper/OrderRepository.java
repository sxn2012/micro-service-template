package com.example.microservice.orderservice.mapper;

import com.example.microservice.model.Order;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends CrudRepository<Order, Long>, PagingAndSortingRepository<Order, Long> {
    List<Order> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE \"order\" SET status = :status WHERE id = :id AND status = :expected")
    int updateStatusIfMatches(@Param("id") Long id, @Param("status") String status, @Param("expected") String expected);
}
