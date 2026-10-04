package com.ritangkar.apiperf.config;

import com.ritangkar.apiperf.entity.OrderIndexed;
import com.ritangkar.apiperf.entity.OrderUnindexed;
import com.ritangkar.apiperf.entity.Product;
import com.ritangkar.apiperf.repository.OrderIndexedRepository;
import com.ritangkar.apiperf.repository.OrderUnindexedRepository;
import com.ritangkar.apiperf.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates synthetic commerce data with a fixed seed, so the row count and
 * exact contents are identical on every run — a requirement for the
 * benchmark tests to be reproducible (see docs/design-decisions.md).
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final long SEED = 20260918L;
    private static final String[] STATUSES = {"PLACED", "SHIPPED", "DELIVERED", "CANCELLED", "RETURNED"};
    private static final String[] CATEGORIES = {"Footwear", "Apparel", "Accessories", "Outdoor Gear", "Electronics"};
    private static final int BATCH_SIZE = 1000;

    private final OrderIndexedRepository orderIndexedRepository;
    private final OrderUnindexedRepository orderUnindexedRepository;
    private final ProductRepository productRepository;
    private final int orderCount;
    private final int productCount;

    public DataInitializer(OrderIndexedRepository orderIndexedRepository,
                            OrderUnindexedRepository orderUnindexedRepository,
                            ProductRepository productRepository,
                            @Value("${apiperf.data.order-count:50000}") int orderCount,
                            @Value("${apiperf.data.product-count:50000}") int productCount) {
        this.orderIndexedRepository = orderIndexedRepository;
        this.orderUnindexedRepository = orderUnindexedRepository;
        this.productRepository = productRepository;
        this.orderCount = orderCount;
        this.productCount = productCount;
    }

    @Override
    public void run(String... args) {
        if (orderIndexedRepository.count() > 0) {
            log.info("Data already present ({} orders) — skipping generation.", orderIndexedRepository.count());
            return;
        }

        long start = System.currentTimeMillis();
        generateOrders();
        generateProducts();
        log.info("Generated {} orders (x2 tables) and {} products in {} ms",
                orderCount, productCount, System.currentTimeMillis() - start);
    }

    private void generateOrders() {
        Random rng = new Random(SEED);
        List<OrderIndexed> indexedBatch = new ArrayList<>(BATCH_SIZE);
        List<OrderUnindexed> unindexedBatch = new ArrayList<>(BATCH_SIZE);

        for (long i = 0; i < orderCount; i++) {
            long customerId = 1 + rng.nextInt(10_000);
            String status = STATUSES[rng.nextInt(STATUSES.length)];
            double total = 200 + rng.nextDouble() * 8800;
            String createdAt = "2026-01-01";

            indexedBatch.add(new OrderIndexed(i, customerId, status, round2(total), createdAt));
            unindexedBatch.add(new OrderUnindexed(i, customerId, status, round2(total), createdAt));

            if (indexedBatch.size() == BATCH_SIZE) {
                orderIndexedRepository.saveAll(indexedBatch);
                orderUnindexedRepository.saveAll(unindexedBatch);
                indexedBatch.clear();
                unindexedBatch.clear();
            }
        }
        if (!indexedBatch.isEmpty()) {
            orderIndexedRepository.saveAll(indexedBatch);
            orderUnindexedRepository.saveAll(unindexedBatch);
        }
    }

    private void generateProducts() {
        Random rng = new Random(SEED + 1);
        List<Product> batch = new ArrayList<>(BATCH_SIZE);

        for (long i = 0; i < productCount; i++) {
            String category = CATEGORIES[rng.nextInt(CATEGORIES.length)];
            double price = 200 + rng.nextDouble() * 8800;
            int stock = rng.nextInt(300);

            batch.add(new Product(i, "Product " + i, category, round2(price), stock,
                    "A synthetic catalog item used for pagination and caching benchmarks."));

            if (batch.size() == BATCH_SIZE) {
                productRepository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            productRepository.saveAll(batch);
        }
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
