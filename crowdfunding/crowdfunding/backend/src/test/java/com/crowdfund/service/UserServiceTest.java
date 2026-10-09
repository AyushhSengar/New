package com.crowdfund.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.crowdfund.dto.RegisterRequest;
import com.crowdfund.dto.RoleRequest;
import com.crowdfund.exception.AppException;
import com.crowdfund.security.AuthUser;
import com.google.cloud.firestore.Firestore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private Firestore db;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("register rejects users who already have a profile")
    void testRegisterDuplicateProfile() {
        AuthUser existingUser = new AuthUser("uid1", "user@test.com", "Name", "CONTRIBUTOR");
        RegisterRequest req = new RegisterRequest("Name", "CONTRIBUTOR");

        AppException ex = assertThrows(AppException.class,
                () -> userService.register(existingUser, req));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    @DisplayName("register rejects attempts to self-assign ADMIN role")
    void testRegisterAdminForbidden() {
        AuthUser newUser = new AuthUser("uid2", "user@test.com", "Name", null);
        RegisterRequest req = new RegisterRequest("Name", "ADMIN");

        AppException ex = assertThrows(AppException.class,
                () -> userService.register(newUser, req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("changeRole blocks non-ADMIN callers")
    void testChangeRoleNonAdmin() {
        AuthUser creator = new AuthUser("uid3", "creator@test.com", "Creator", "CREATOR");
        RoleRequest req = new RoleRequest("CONTRIBUTOR");

        AppException ex = assertThrows(AppException.class,
                () -> userService.changeRole(creator, "uid4", req));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    @DisplayName("changeRole blocks admins from modifying their own role")
    void testChangeRoleSelfBlocked() {
        AuthUser admin = new AuthUser("uid5", "admin@test.com", "Admin", "ADMIN");
        RoleRequest req = new RoleRequest("CREATOR");

        AppException ex = assertThrows(AppException.class,
                () -> userService.changeRole(admin, "uid5", req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("You cannot change your own role.", ex.getMessage());
    }
}
