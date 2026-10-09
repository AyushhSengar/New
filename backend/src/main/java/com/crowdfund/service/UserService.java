package com.crowdfund.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.crowdfund.dto.RegisterRequest;
import com.crowdfund.dto.RoleRequest;
import com.crowdfund.dto.UserSummaryResponse;
import com.crowdfund.exception.AppException;
import com.crowdfund.security.AuthUser;
import com.crowdfund.util.FirestoreSupport;
import com.crowdfund.util.Text;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;

@Service
public class UserService {

    private static final Set<String> ALL_ROLES = Set.of("ADMIN", "CREATOR", "CONTRIBUTOR");

    private final Firestore db;

    public UserService(Firestore db) {
        this.db = db;
    }

    /**
     * ADMIN only: Lists all registered users sorted alphabetically by name/email.
     */
    public List<UserSummaryResponse> listUsers(AuthUser caller) {
        caller.requireRole("ADMIN");

        List<QueryDocumentSnapshot> docs = FirestoreSupport.await(db.collection("users").get()).getDocuments();

        return docs.stream().map(doc -> {
            String uid = doc.getId();
            String name = doc.getString("name");
            String email = doc.getString("email");
            String role = doc.getString("ROLE");
            Timestamp created = doc.getTimestamp("createdAt");
            Instant createdAt = created != null ? created.toDate().toInstant() : null;
            return new UserSummaryResponse(uid, name, email, role, createdAt);
        }).sorted(Comparator.comparing(u -> {
            String val = u.name() != null && !u.name().isBlank() ? u.name() : (u.email() != null ? u.email() : "");
            return val.toLowerCase();
        })).collect(Collectors.toList());
    }

    /**
     * Creates the Firestore profile for a freshly signed-up Firebase user.
     * Only CREATOR or CONTRIBUTOR can be chosen here; ADMIN is never self-assigned.
     */
    public Map<String, Object> register(AuthUser caller, RegisterRequest request) {

        if (caller.hasProfile()) {
            throw new AppException(HttpStatus.CONFLICT, "A profile already exists for this account.");
        }

        String name = Text.require(request.name(), "Name", 80);
        String role = normalizeRole(request.role());

        if (!role.equals("CREATOR") && !role.equals("CONTRIBUTOR")) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Please choose Creator or Contributor.");
        }

        DocumentReference ref = db.collection("users").document(caller.uid());

        FirestoreSupport.transaction(db, transaction -> {

            DocumentSnapshot existing = transaction.get(ref).get();

            if (existing.exists()) {
                throw new AppException(HttpStatus.CONFLICT,
                        "A profile already exists for this account.");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("name", name);
            data.put("email", caller.email() == null ? "" : caller.email());
            data.put("ROLE", role);
            data.put("createdAt", FieldValue.serverTimestamp());
            data.put("updatedAt", FieldValue.serverTimestamp());

            transaction.set(ref, data);

            return null;
        });

        return Map.of("uid", caller.uid(), "role", role);
    }

    /** ADMIN only. Admins cannot change their own role (prevents locking everyone out). */
    public Map<String, Object> changeRole(AuthUser caller, String targetUid, RoleRequest request) {

        caller.requireRole("ADMIN");

        Text.requireId(targetUid);

        String role = normalizeRole(request.role());

        if (!ALL_ROLES.contains(role)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Invalid role selected.");
        }

        if (targetUid.equals(caller.uid())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "You cannot change your own role.");
        }

        DocumentReference ref = db.collection("users").document(targetUid);

        FirestoreSupport.transaction(db, transaction -> {

            DocumentSnapshot snapshot = transaction.get(ref).get();

            if (!snapshot.exists()) {
                throw new AppException(HttpStatus.NOT_FOUND, "User not found.");
            }

            Map<String, Object> update = new HashMap<>();
            update.put("ROLE", role);
            update.put("updatedAt", FieldValue.serverTimestamp());

            transaction.update(ref, update);

            return null;
        });

        return Map.of("uid", targetUid, "role", role);
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    }
}
