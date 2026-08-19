package com.example.product.controller;

import com.example.product.model.ProductEntity;
import com.example.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    @GetMapping
    public List<ProductEntity> getAllProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        Optional<ProductEntity> productOpt = productRepository.findById(id);
        if (productOpt.isPresent()) {
            return ResponseEntity.ok(productOpt.get());
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Product not found"));
    }

    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody ProductEntity product) {
        if (product.getName() == null || product.getPrice() == null || product.getStock() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Name, price, and stock are required"));
        }
        ProductEntity saved = productRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PostMapping("/reduce-stock")
    public ResponseEntity<?> reduceStock(@RequestBody Map<String, Object> request) {
        Long productId = Long.valueOf(request.get("productId").toString());
        Integer quantity = Integer.valueOf(request.get("quantity").toString());

        Optional<ProductEntity> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Product not found"));
        }

        ProductEntity product = productOpt.get();
        if (product.getStock() < quantity) {
            return ResponseEntity.badRequest().body(Map.of("message", "Insufficient stock for " + product.getName()));
        }

        product.setStock(product.getStock() - quantity);
        productRepository.save(product);

        return ResponseEntity.ok(Map.of(
                "message", "Stock reduced successfully",
                "productId", productId,
                "remainingStock", product.getStock()
        ));
    }
}
