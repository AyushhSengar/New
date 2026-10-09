package com.crowdfund.security;

import org.springframework.http.HttpStatus;

import com.crowdfund.exception.AppException;

/**
 * The verified caller of a request. The role comes from Firestore
 * (users/{uid}.ROLE) on the server, never from anything the browser sends.
 * role is null when the user has signed up but has no profile yet.
 */
public record AuthUser(String uid, String email, String name, String role) {

    public boolean hasProfile() {
        return role != null && !role.isBlank();
    }

    public void requireRole(String... allowedRoles) {

        if (!hasProfile()) {
            throw new AppException(HttpStatus.FORBIDDEN,
                    "No profile found for this account. Please contact the administrator.");
        }

        for (String allowed : allowedRoles) {
            if (allowed.equals(role)) {
                return;
            }
        }

        throw new AppException(HttpStatus.FORBIDDEN,
                "You do not have permission to perform this action.");
    }

    public String displayName() {

        if (name != null && !name.isBlank()) {
            return name;
        }

        if (email != null && !email.isBlank()) {
            return email;
        }

        return "User";
    }
}
