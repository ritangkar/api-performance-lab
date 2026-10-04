package com.ritangkar.apiperf.repository;

import com.ritangkar.apiperf.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // findAll(Pageable) is inherited from JpaRepository/PagingAndSortingRepository —
    // no custom method needed; ProductController uses it directly.
    Page<Product> findByCategory(String category, Pageable pageable);
}
