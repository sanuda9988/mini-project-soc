package com.example.product;

import com.example.product.model.ProductEntity;
import com.example.product.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProductApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(ProductRepository productRepository) {
        return args -> {
            productRepository.save(new ProductEntity("iPhone 15 Pro Max", 1199.00, 50, "Titanium design, A17 Pro chip"));
            productRepository.save(new ProductEntity("MacBook Pro 16\"", 2499.00, 20, "M3 Max chip, 36GB RAM, 1TB SSD"));
            productRepository.save(new ProductEntity("Sony WH-1000XM5", 399.00, 100, "Industry leading noise canceling headphones"));
            productRepository.save(new ProductEntity("iPad Pro 12.9\"", 1099.00, 35, "M2 chip, Liquid Retina XDR display"));
            productRepository.save(new ProductEntity("Apple Watch Ultra 2", 799.00, 60, "Rugged GPS + Cellular watch"));
        };
    }
}
