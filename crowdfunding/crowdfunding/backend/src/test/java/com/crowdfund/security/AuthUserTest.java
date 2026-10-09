package com.crowdfund.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.crowdfund.exception.AppException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthUserTest {

    @Test
    @DisplayName("hasProfile returns true only when role is non-empty")
    void testHasProfile() {
        AuthUser withProfile = new AuthUser("uid1", "test@test.com", "Test User", "CREATOR");
        assertTrue(withProfile.hasProfile());

        AuthUser withoutProfile = new AuthUser("uid2", "test@test.com", "Test User", null);
        assertFalse(withoutProfile.hasProfile());

        AuthUser blankRole = new AuthUser("uid3", "test@test.com", "Test User", "  ");
        assertFalse(blankRole.hasProfile());
    }

    @Test
    @DisplayName("requireRole succeeds when role matches and throws FORBIDDEN otherwise")
    void testRequireRole() {
        AuthUser admin = new AuthUser("uid1", "admin@test.com", "Admin", "ADMIN");
        assertDoesNotThrow(() -> admin.requireRole("ADMIN"));
        assertDoesNotThrow(() -> admin.requireRole("CREATOR", "ADMIN"));

        AppException ex = assertThrows(AppException.class, () -> admin.requireRole("CREATOR"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("displayName falls back gracefully from name to email to User")
    void testDisplayName() {
        AuthUser full = new AuthUser("uid1", "email@test.com", "Full Name", "CONTRIBUTOR");
        assertEquals("Full Name", full.displayName());

        AuthUser noName = new AuthUser("uid2", "email@test.com", null, "CONTRIBUTOR");
        assertEquals("email@test.com", noName.displayName());

        AuthUser nothing = new AuthUser("uid3", null, null, "CONTRIBUTOR");
        assertEquals("User", nothing.displayName());
    }
}
