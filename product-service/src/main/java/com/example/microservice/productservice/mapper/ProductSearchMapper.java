package com.example.microservice.productservice.mapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.example.microservice.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ProductSearchMapper {

    private static final String INDEX = "product";

    private final ElasticsearchClient client;

    public void index(Product product) {
        ProductDocument doc = new ProductDocument();
        doc.setId(product.getId());
        doc.setName(product.getName());
        doc.setDescription(product.getDescription());
        doc.setPrice(product.getPrice() == null ? null : product.getPrice().doubleValue());
        doc.setCategoryId(product.getCategoryId());
        try {
            client.index(i -> i
                    .index(INDEX)
                    .id(String.valueOf(product.getId()))
                    .document(doc));
        } catch (IOException e) {
            throw new RuntimeException("Failed to index product " + product.getId(), e);
        }
    }

    public void delete(Long id) {
        try {
            client.delete(d -> d.index(INDEX).id(String.valueOf(id)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete product index " + id, e);
        }
    }

    public List<ProductDocument> search(String keyword) {
        try {
            SearchResponse<ProductDocument> response = client.search(s -> s
                            .index(INDEX)
                            .query(q -> q.multiMatch(mm -> mm
                                    .query(keyword)
                                    .fields("name", "description"))),
                    ProductDocument.class);
            return response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to search products", e);
        }
    }
}
