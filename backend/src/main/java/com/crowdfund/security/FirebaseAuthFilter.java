package com.crowdfund.security;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Runs before every /api/** request (except /api/health):
 *  1. reads "Authorization: Bearer <Firebase ID token>"
 *  2. verifies the token with the Firebase Admin SDK
 *  3. loads the caller's role from Firestore
 *  4. exposes an AuthUser to the controllers
 */
@Component
public class FirebaseAuthFilter extends OncePerRequestFilter {

    public static final String ATTRIBUTE = "authUser";

    private static final Logger log = LoggerFactory.getLogger(FirebaseAuthFilter.class);

    private final FirebaseAuth firebaseAuth;
    private final Firestore db;
    private final ObjectMapper mapper;

    public FirebaseAuthFilter(FirebaseAuth firebaseAuth, Firestore db, ObjectMapper mapper) {
        this.firebaseAuth = firebaseAuth;
        this.db = db;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getRequestURI();

        return !path.startsWith("/api/")
                || path.equals("/api/health")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            reject(response, 401, "Please sign in to continue.");
            return;
        }

        FirebaseToken token;

        try {
            token = firebaseAuth.verifyIdToken(header.substring(7).trim());

        } catch (FirebaseAuthException | IllegalArgumentException e) {
            log.debug("Token verification failed: {}", e.getMessage());
            reject(response, 401, "Your session is invalid or has expired. Please sign in again.");
            return;
        }

        String role = null;
        String name = token.getName();

        try {
            DocumentSnapshot profile = db.collection("users").document(token.getUid()).get().get();

            if (profile.exists()) {

                String storedRole = profile.getString("ROLE");

                if (storedRole != null) {
                    role = storedRole.trim().toUpperCase(Locale.ROOT);
                }

                String storedName = profile.getString("name");

                if (storedName != null && !storedName.isBlank()) {
                    name = storedName;
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            reject(response, 503, "Request interrupted. Please try again.");
            return;

        } catch (ExecutionException e) {
            log.error("Could not load user profile for {}", token.getUid(), e);
            reject(response, 503, "The database is temporarily unavailable. Please try again.");
            return;
        }

        request.setAttribute(ATTRIBUTE,
                new AuthUser(token.getUid(), token.getEmail(), name, role));

        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int status, String message)
            throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(mapper.writeValueAsString(Map.of("error", message)));
    }
}
