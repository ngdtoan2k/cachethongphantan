package com.ecommerce.user.config;

import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
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

    private final UserRepository userRepository;

    @Bean
    public CommandLineRunner loadUserData() {
        return args -> {
            // Tạo tài khoản Admin mặc định nếu chưa có
            if (userRepository.findByEmail("admin@shop.com").isEmpty()) {
                User admin = User.builder()
                        .fullName("Admin User")
                        .email("admin@shop.com")
                        .password("admin123")
                        .role("ROLE_ADMIN")
                        .build();
                userRepository.save(admin);
                log.info("Tạo tài khoản Admin thành công: admin@shop.com / admin123");
            }

            // Tạo user mẫu
            if (userRepository.findByEmail("john@example.com").isEmpty()) {
                User user = User.builder()
                        .fullName("John Doe")
                        .email("john@example.com")
                        .password("password123")
                        .role("ROLE_USER")
                        .build();
                userRepository.save(user);
                log.info("Tạo user mẫu thành công: john@example.com / password123");
            }
        };
    }
}
