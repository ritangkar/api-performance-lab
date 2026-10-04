package com.ritangkar.apiperf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * API Performance Lab.
 *
 * A small commerce backend built specifically to demonstrate — and let you
 * reproduce — four measurable backend performance techniques: indexing,
 * caching, pagination and concurrent processing. See docs/design-decisions.md
 * for the real, measured numbers behind each one and exactly how they were
 * produced (nothing here is a fabricated benchmark).
 */
@SpringBootApplication
@EnableCaching
public class ApiPerformanceLabApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiPerformanceLabApplication.class, args);
    }
}
