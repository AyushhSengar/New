package com.crowdfund.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing user summary in FundHub.
 */
public class UserSummary {

    private String uid;
    private String name;
    private String email;
    private String role; // ADMIN, CREATOR, CONTRIBUTOR
    private Instant createdAt;

    public UserSummary() {
    }

    public UserSummary(String uid, String name, String email, String role, Instant createdAt) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSummary that = (UserSummary) o;
        return Objects.equals(uid, that.uid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uid);
    }
}
