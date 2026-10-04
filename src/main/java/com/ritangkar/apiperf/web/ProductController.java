package com.ritangkar.apiperf.web;

import com.ritangkar.apiperf.entity.Product;
import com.ritangkar.apiperf.repository.ProductRepository;
import com.ritangkar.apiperf.service.ProductCatalogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductCatalogService catalogService;

    public ProductController(ProductRepository productRepository, ProductCatalogService catalogService) {
        this.productRepository = productRepository;
        this.catalogService = catalogService;
    }

    /**
     * ANTI-PATTERN, kept intentionally for the benchmark: returns the
     * entire catalog in one response. See PaginationBenchmarkTest and
     * docs/design-decisions.md for the measured payload-size and
     * serialization-time cost of this endpoint versus the one below.
     */
    @GetMapping("/all")
    public List<Product> allProductsNaive() {
        return productRepository.findAll();
    }

    /** Recommended: page-based, bounded response size regardless of catalog size. */
    @GetMapping
    public Page<Product> products(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        int boundedSize = Math.max(1, Math.min(size, 100));
        return productRepository.findAll(PageRequest.of(page, boundedSize));
    }

    /** Cached hot-path lookup — see ProductCatalogService and CachingBenchmarkTest. */
    @GetMapping("/{id}")
    public ProductCatalogService.ProductDetails productDetails(@PathVariable Long id) {
        return catalogService.getProductDetails(id);
    }
}
