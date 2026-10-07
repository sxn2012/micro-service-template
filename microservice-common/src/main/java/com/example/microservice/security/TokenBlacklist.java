package com.example.microservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;
import java.util.HexFormat;

@Slf4j
@Component
public class TokenBlacklist {

    private static final String KEY_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklist(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    public void revoke(String token, Date expiration) {
        if (redisTemplate == null || token == null || expiration == null) {
            return;
        }
        long ttlMillis = expiration.getTime() - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key(token), "1", Duration.ofMillis(ttlMillis));
        } catch (Exception e) {
            log.warn("Failed to blacklist token, logout revocation unavailable: {}", e.getMessage());
        }
    }

    public boolean isRevoked(String token) {
        if (redisTemplate == null || token == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key(token)));
        } catch (Exception e) {
            log.warn("Token blacklist check failed, failing open: {}", e.getMessage());
            return false;
        }
    }

    private String key(String token) {
        return KEY_PREFIX + sha256(token);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
