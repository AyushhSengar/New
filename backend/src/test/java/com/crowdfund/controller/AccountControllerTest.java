package com.crowdfund.controller;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.crowdfund.dto.RegisterRequest;
import com.crowdfund.dto.RoleRequest;
import com.crowdfund.dto.UserSummaryResponse;
import com.crowdfund.security.AuthUser;
import com.crowdfund.service.UserService;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private UserService userService;

    private AccountController controller;

    @BeforeEach
    void setUp() {
        controller = new AccountController(userService);
    }

    @Test
    void testHealth() {
        Map<String, String> res = controller.health();
        assertEquals("ok", res.get("status"));
    }

    @Test
    void testMe() {
        AuthUser user = new AuthUser("u1", "user@test.com", "Test User", "CREATOR");
        Map<String, Object> me = controller.me(user);
        assertEquals("u1", me.get("uid"));
        assertEquals("user@test.com", me.get("email"));
        assertEquals("CREATOR", me.get("role"));
    }

    @Test
    void testRegister() {
        AuthUser user = new AuthUser("u1", "user@test.com", "Test User", "ANONYMOUS");
        RegisterRequest req = new RegisterRequest("Test User", "CREATOR");
        when(userService.register(user, req)).thenReturn(Map.of("uid", "u1", "role", "CREATOR"));

        Map<String, Object> res = controller.register(user, req);
        assertEquals("u1", res.get("uid"));
        assertEquals("CREATOR", res.get("role"));
    }

    @Test
    void testListUsers() {
        AuthUser admin = new AuthUser("admin_id", "admin@test.com", "Admin", "ADMIN");
        UserSummaryResponse u = new UserSummaryResponse("u1", "Alice", "alice@test.com", "CREATOR", null);
        when(userService.listUsers(admin)).thenReturn(List.of(u));

        List<UserSummaryResponse> list = controller.listUsers(admin);
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Alice", list.get(0).name());
    }

    @Test
    void testChangeRole() {
        AuthUser admin = new AuthUser("admin_id", "admin@test.com", "Admin", "ADMIN");
        RoleRequest req = new RoleRequest("CREATOR");
        when(userService.changeRole(admin, "u1", req)).thenReturn(Map.of("uid", "u1", "role", "CREATOR"));

        Map<String, Object> res = controller.changeRole(admin, "u1", req);
        assertEquals("CREATOR", res.get("role"));
    }
}
