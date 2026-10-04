package com.ritangkar.apiperf.service;

import com.ritangkar.apiperf.entity.Product;
import com.ritangkar.apiperf.repository.ProductRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
public class ProductCatalogService {

    private final ProductRepository productRepository;

    public ProductCatalogService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Returns a product's details plus a computed "relevance score" —
     * standing in for whatever expensive, deterministic-but-costly
     * computation a real catalog lookup might do (aggregating reviews,
     * scoring recommendations, joining pricing rules). The computation
     * itself is real CPU work (not a sleep), so caching it has a genuine,
     * measurable effect — see CachingBenchmarkTest.
     */
    @Cacheable("productDetails")
    public ProductDetails getProductDetails(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Unknown product id: " + id));
        double score = computeExpensiveRelevanceScore(product);
        return new ProductDetails(product.getId(), product.getName(), product.getCategory(),
                product.getPrice(), product.getStockQuantity(), score);
    }

    /**
     * Deliberately non-trivial CPU work, structurally identical to the
     * standalone benchmark in docs/design-decisions.md, so the documented
     * measured speedup is representative of what this method actually does.
     */
    private double computeExpensiveRelevanceScore(Product product) {
        double acc = 0;
        for (int i = 0; i < 2_000_000; i++) {
            acc += Math.sqrt(product.getId() + i) * Math.sin(i);
        }
        return acc;
    }

    public record ProductDetails(Long id, String name, String category, Double price,
                                  Integer stockQuantity, double relevanceScore) {
    }
}
