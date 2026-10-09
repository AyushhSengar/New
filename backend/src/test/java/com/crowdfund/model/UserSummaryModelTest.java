package com.crowdfund.model;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;

class UserSummaryModelTest {

    @Test
    void testGettersAndEquality() {
        Instant now = Instant.now();
        UserSummary u1 = new UserSummary("u1", "John", "john@test.com", "CREATOR", now);
        UserSummary u2 = new UserSummary("u1", "John", "john@test.com", "CREATOR", now);
        UserSummary u3 = new UserSummary("u2", "John", "john@test.com", "CREATOR", now);

        assertEquals(u1, u2);
        assertNotEquals(u1, u3);
        assertEquals(u1.hashCode(), u2.hashCode());
        assertEquals("u1", u1.getUid());
        assertEquals("John", u1.getName());
        assertEquals("john@test.com", u1.getEmail());
        assertEquals("CREATOR", u1.getRole());
        assertEquals(now, u1.getCreatedAt());
    }
}
