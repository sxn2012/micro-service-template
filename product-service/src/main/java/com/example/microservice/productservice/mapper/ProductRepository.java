package com.example.microservice.productservice.mapper;

import com.example.microservice.model.Product;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends CrudRepository<Product, Long>, PagingAndSortingRepository<Product, Long> {
    List<Product> findByCategoryId(Long categoryId);

    @Modifying
    @Query("UPDATE product SET stock = stock - :quantity WHERE id = :id AND stock >= :quantity")
    int decrementStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE product SET stock = stock + :quantity WHERE id = :id")
    int incrementStock(@Param("id") Long id, @Param("quantity") int quantity);
}
