package com.example.microservice.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "ideaTestSecretKeyForJwt2026MustBeAtLeast256BitsLong!!";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 86_400_000L);

    @Test
    void generatesValidTokenWithClaims() {
        String token = jwtUtil.generate(42L, "alice", "ADMIN");

        assertThat(jwtUtil.validate(token)).isTrue();
        Claims claims = jwtUtil.parse(token);
        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(((Number) claims.get("userId")).longValue()).isEqualTo(42L);
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtUtil.generate(1L, "bob", "USER");
        String corrupted = token.substring(0, token.length() - 1)
                + (token.charAt(token.length() - 1) == 'a' ? 'b' : 'a');
        assertThat(jwtUtil.validate(corrupted)).isFalse();
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        JwtUtil other = new JwtUtil("anotherSecretKeyForJwt2026MustBeAtLeast256BitsLong!!", 86_400_000L);
        String token = other.generate(1L, "bob", "USER");
        assertThat(jwtUtil.validate(token)).isFalse();
    }

    @Test
    void rejectsGarbageToken() {
        assertThat(jwtUtil.validate("not.a.jwt")).isFalse();
        assertThat(jwtUtil.validate("")).isFalse();
    }
}
