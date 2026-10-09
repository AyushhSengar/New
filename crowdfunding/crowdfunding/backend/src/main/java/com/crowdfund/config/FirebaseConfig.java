package com.crowdfund.config;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;

/**
 * Creates the Firebase Admin SDK beans. The Admin SDK bypasses Firestore
 * security rules, which is why every request is authorised in Java first.
 */
@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.project-id}")
    private String projectId;

    @Value("${app.firebase.service-account-json:}")
    private String serviceAccountJson;

    @Value("${app.firebase.service-account-path:}")
    private String serviceAccountPath;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {

        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        GoogleCredentials credentials;

        if (!serviceAccountJson.isBlank()) {

            credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(
                            serviceAccountJson.getBytes(StandardCharsets.UTF_8)));

        } else if (!serviceAccountPath.isBlank()) {

            try (InputStream input = new FileInputStream(serviceAccountPath)) {
                credentials = GoogleCredentials.fromStream(input);
            }

        } else {
            throw new IllegalStateException(
                    "Firebase credentials missing. Set FIREBASE_SERVICE_ACCOUNT_PATH "
                            + "(local file) or FIREBASE_SERVICE_ACCOUNT_JSON (hosting).");
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();

        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp app) {
        return FirebaseAuth.getInstance(app);
    }

    @Bean
    public Firestore firestore(FirebaseApp app) {
        return FirestoreClient.getFirestore(app);
    }
}
