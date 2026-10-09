package com.crowdfund.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.crowdfund.dto.RegisterRequest;
import com.crowdfund.dto.RoleRequest;
import com.crowdfund.dto.UserSummaryResponse;
import com.crowdfund.security.AuthUser;
import com.crowdfund.security.FirebaseAuthFilter;
import com.crowdfund.service.UserService;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final UserService userService;

    public AccountController(UserService userService) {
        this.userService = userService;
    }

    /** Public: lets you check that the backend is running. */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    /** Returns the verified caller (useful to test token verification). */
    @GetMapping("/me")
    public Map<String, Object> me(@RequestAttribute(FirebaseAuthFilter.ATTRIBUTE) AuthUser user) {

        Map<String, Object> body = new HashMap<>();
        body.put("uid", user.uid());
        body.put("email", user.email());
        body.put("name", user.displayName());
        body.put("role", user.role());
        body.put("hasProfile", user.hasProfile());

        return body;
    }

    @PostMapping("/register")
    public Map<String, Object> register(
            @RequestAttribute(FirebaseAuthFilter.ATTRIBUTE) AuthUser user,
            @RequestBody RegisterRequest request) {

        return userService.register(user, request);
    }

    @GetMapping("/users")
    public List<UserSummaryResponse> listUsers(
            @RequestAttribute(FirebaseAuthFilter.ATTRIBUTE) AuthUser user) {

        return userService.listUsers(user);
    }

    @PatchMapping("/users/{uid}/role")
    public Map<String, Object> changeRole(
            @RequestAttribute(FirebaseAuthFilter.ATTRIBUTE) AuthUser user,
            @PathVariable String uid,
            @RequestBody RoleRequest request) {

        return userService.changeRole(user, uid, request);
    }
}
