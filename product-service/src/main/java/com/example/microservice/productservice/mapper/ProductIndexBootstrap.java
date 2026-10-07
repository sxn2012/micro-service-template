package com.example.microservice.productservice.mapper;

import com.example.microservice.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductIndexBootstrap implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final ProductSearchMapper productSearchMapper;

    @Override
    public void run(ApplicationArguments args) {
        List<Product> products = new ArrayList<>();
        productRepository.findAll().forEach(products::add);
        products.forEach(productSearchMapper::index);
        log.info("Reindexed {} products into Elasticsearch", products.size());
    }
}
