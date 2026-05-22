package com.ecommerce.product.config;

import com.ecommerce.product.entity.Product;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final ProductRepository productRepository;

    @Bean
    public CommandLineRunner loadProductData() {
        return args -> {
            if (productRepository.count() == 0) {
                log.info("Khởi tạo dữ liệu mẫu cho Product Service...");
                productRepository.saveAll(List.of(
                        Product.builder().name("MacBook Pro M3").description("Laptop cao cấp của Apple").price(1999.99).stockQuantity(50).build(),
                        Product.builder().name("iPhone 15 Pro Max").description("Điện thoại Apple mới nhất").price(1199.00).stockQuantity(100).build(),
                        Product.builder().name("Samsung Galaxy S24 Ultra").description("Điện thoại cao cấp của Samsung").price(1299.00).stockQuantity(80).build(),
                        Product.builder().name("Sony WH-1000XM5").description("Tai nghe chống ồn Sony").price(348.00).stockQuantity(120).build(),
                        Product.builder().name("Apple Watch Ultra 2").description("Đồng hồ thông minh siêu bền").price(799.00).stockQuantity(40).build(),
                        Product.builder().name("iPad Pro M4 2024").description("Máy tính bảng Apple mỏng nhẹ").price(1099.00).stockQuantity(60).build()
                ));
                log.info("Đã tạo xong dữ liệu sản phẩm mẫu!");
            }
        };
    }
}
