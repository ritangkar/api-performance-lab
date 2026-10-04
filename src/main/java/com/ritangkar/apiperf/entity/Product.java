package com.ritangkar.apiperf.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Product {

    @Id
    private Long id;

    private String name;
    private String category;
    private Double price;
    private Integer stockQuantity;
    private String description;

    protected Product() {
    }

    public Product(Long id, String name, String category, Double price, Integer stockQuantity, String description) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public Double getPrice() { return price; }
    public Integer getStockQuantity() { return stockQuantity; }
    public String getDescription() { return description; }
}
