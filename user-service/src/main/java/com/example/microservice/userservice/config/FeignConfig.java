package com.example.microservice.userservice.config;

import com.example.microservice.security.JwtAuthFilter;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor internalTokenInterceptor(@Value("${internal.token:}") String internalToken) {
        return template -> template.header(JwtAuthFilter.INTERNAL_TOKEN_HEADER, internalToken);
    }
}
