package com.example.microservice.security;

import com.example.microservice.exception.ForbiddenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityUtilsTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void ownerCanAccessOwnResource() {
        setPrincipal(1L, "USER");
        assertThatCode(() -> SecurityUtils.requireOwnerOrAdmin(1L)).doesNotThrowAnyException();
    }

    @Test
    void otherUserCannotAccess() {
        setPrincipal(1L, "USER");
        assertThatThrownBy(() -> SecurityUtils.requireOwnerOrAdmin(2L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void adminCanAccessAnyUser() {
        setPrincipal(9L, "ADMIN");
        assertThatCode(() -> SecurityUtils.requireOwnerOrAdmin(2L)).doesNotThrowAnyException();
    }

    @Test
    void requireAdminRejectsUser() {
        setPrincipal(1L, "USER");
        assertThatThrownBy(SecurityUtils::requireAdmin).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void unauthenticatedIsForbidden() {
        assertThatThrownBy(() -> SecurityUtils.requireOwnerOrAdmin(1L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getCurrentUserIdReturnsPrincipalId() {
        setPrincipal(7L, "USER");
        assertThatCode(() -> {
            Long id = SecurityUtils.getCurrentUserId();
            if (!id.equals(7L)) {
                throw new AssertionError("expected 7L but was " + id);
            }
        }).doesNotThrowAnyException();
    }

    private void setPrincipal(Long id, String role) {
        var authentication = new UsernamePasswordAuthenticationToken(
                new CurrentUser(id, "user" + id, role), null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
