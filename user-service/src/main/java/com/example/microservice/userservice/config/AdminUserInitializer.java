package com.example.microservice.userservice.config;

import com.example.microservice.model.User;
import com.example.microservice.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements ApplicationRunner {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.username:admin}")
    private String username;

    @Value("${admin.password:admin123}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (userService.getUserByUsername(username).isPresent()) {
            return;
        }
        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setEmail("admin@test.com");
        admin.setRole("ADMIN");
        admin.setCreatedAt(LocalDateTime.now());
        userService.save(admin);
        log.info("Initialized default admin user '{}'", username);
    }
}
