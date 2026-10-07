package com.example.microservice.productservice.service;

import com.example.microservice.exception.BusinessException;
import com.example.microservice.message.MqEvent;
import com.example.microservice.message.ProductIndexEvent;
import com.example.microservice.message.RabbitTopics;
import com.example.microservice.message.StockEventMessage;
import com.example.microservice.model.Product;
import com.example.microservice.productservice.mapper.ProductDocument;
import com.example.microservice.productservice.mapper.ProductRepository;
import com.example.microservice.productservice.mapper.ProductSearchMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ProductSearchMapper productSearchMapper = mock(ProductSearchMapper.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final ProductService productService =
            new ProductService(productRepository, productSearchMapper, eventPublisher);

    @Test
    @SuppressWarnings("unchecked")
    void searchPreservesEsRankingOrder() {
        when(productSearchMapper.search("shoe")).thenReturn(List.of(doc(3L), doc(1L), doc(2L)));
        when(productRepository.findAllById(any())).thenReturn(List.of(product(1L), product(2L), product(3L)));

        List<Product> result = productService.searchProducts("shoe");

        assertThat(result.stream().map(Product::getId).toList()).containsExactly(3L, 1L, 2L);
    }

    @Test
    void searchReturnsEmptyWhenNoEsHits() {
        when(productSearchMapper.search("nope")).thenReturn(List.of());

        assertThat(productService.searchProducts("nope")).isEmpty();
    }

    @Test
    void decreaseStockPublishesIndexEvent() {
        Product product = product(1L);
        product.setName("Shoe");
        product.setStock(5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.decrementStock(1L, 2)).thenReturn(1);

        productService.decreaseStock(1L, 2);

        verify(eventPublisher).publishEvent(new MqEvent(RabbitTopics.PRODUCT_INDEX,
                new ProductIndexEvent(1L, ProductIndexEvent.IndexAction.INDEX)));
        verify(eventPublisher).publishEvent((Object) argThat(evt -> evt instanceof MqEvent mq
                && RabbitTopics.STOCK_DECREASE.equals(mq.routingKey())
                && mq.payload() instanceof StockEventMessage msg
                && msg.productId().equals(1L)
                && msg.quantity() == 2));
    }

    @Test
    void decreaseStockThrowsWhenInsufficient() {
        Product product = product(1L);
        product.setName("Shoe");
        product.setStock(5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.decrementStock(1L, 10)).thenReturn(0);

        assertThatThrownBy(() -> productService.decreaseStock(1L, 10))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void deleteProductPublishesDeleteEvent() {
        productService.deleteProduct(7L);

        verify(productRepository).deleteById(7L);
        verify(eventPublisher).publishEvent(new MqEvent(RabbitTopics.PRODUCT_DELETE,
                new ProductIndexEvent(7L, ProductIndexEvent.IndexAction.DELETE)));
    }

    private Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        return product;
    }

    private ProductDocument doc(Long id) {
        ProductDocument doc = new ProductDocument();
        doc.setId(id);
        return doc;
    }
}
