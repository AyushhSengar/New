package com.crowdfund.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;

/**
 * Initializes the Firebase Admin SDK for FundHub.
 *
 * Supports credential resolution via:
 * 1. FIREBASE_SERVICE_ACCOUNT_JSON (direct JSON string, ideal for cloud hosting)
 * 2. FIREBASE_SERVICE_ACCOUNT_PATH (file path, ideal for local Windows/macOS development)
 * 3. GOOGLE_APPLICATION_CREDENTIALS (standard GCP environment variable)
 * 4. Application default credentials
 * 5. Safe local development fallback for offline/development testing
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${app.firebase.project-id:crowdingfunding}")
    private String projectId;

    @Value("${app.firebase.service-account-json:}")
    private String serviceAccountJson;

    @Value("${app.firebase.service-account-path:}")
    private String serviceAccountPath;

    @Value("${app.firebase.allow-demo-fallback:true}")
    private boolean allowDemoFallback;

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        GoogleCredentials credentials = resolveCredentials();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(projectId)
                .build();

        log.info("Successfully initialized Firebase Admin SDK for FundHub project [{}]", projectId);
        return FirebaseApp.initializeApp(options);
    }

    private GoogleCredentials resolveCredentials() throws IOException {
        // 1. Direct JSON content (Production / Cloud Hosting)
        if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
            log.info("Loading Firebase credentials from FIREBASE_SERVICE_ACCOUNT_JSON environment variable.");
            return GoogleCredentials.fromStream(
                    new ByteArrayInputStream(serviceAccountJson.getBytes(StandardCharsets.UTF_8)));
        }

        // 2. Specific file path configured via env/properties (Local Windows Development)
        if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
            Path path = Paths.get(serviceAccountPath.trim().replace("\"", ""));
            if (Files.exists(path)) {
                log.info("Loading Firebase credentials from path: {}", path.toAbsolutePath());
                try (InputStream in = Files.newInputStream(path)) {
                    return GoogleCredentials.fromStream(in);
                }
            } else {
                throw new IllegalStateException(
                        "Configured FIREBASE_SERVICE_ACCOUNT_PATH file does not exist: " + path.toAbsolutePath());
            }
        }

        // 3. Standard Google Cloud environment variable
        String gcpCreds = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (gcpCreds != null && !gcpCreds.isBlank()) {
            Path gcpPath = Paths.get(gcpCreds.trim().replace("\"", ""));
            if (Files.exists(gcpPath)) {
                log.info("Loading Firebase credentials from GOOGLE_APPLICATION_CREDENTIALS: {}", gcpPath.toAbsolutePath());
                try (InputStream in = Files.newInputStream(gcpPath)) {
                    return GoogleCredentials.fromStream(in);
                }
            }
        }

        // 4. Check common safe local key locations (e.g. in user home directory outside project)
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            Path[] commonLocations = new Path[] {
                Paths.get(userHome, ".keys", "fundhub-key.json"),
                Paths.get(userHome, "keys", "fundhub-key.json"),
                Paths.get(userHome, ".keys", "crowdfund-key.json"),
                Paths.get(userHome, "keys", "crowdfund-key.json")
            };

            for (Path loc : commonLocations) {
                if (Files.exists(loc)) {
                    log.info("Found local Firebase service account key at: {}", loc.toAbsolutePath());
                    try (InputStream in = Files.newInputStream(loc)) {
                        return GoogleCredentials.fromStream(in);
                    }
                }
            }
        }

        // 5. Try Google Application Default Credentials
        try {
            return GoogleCredentials.getApplicationDefault();
        } catch (Exception e) {
            log.debug("Application Default Credentials not found: {}", e.getMessage());
        }

        // 6. Safe local fallback if allowed
        if (allowDemoFallback) {
            log.warn("----------------------------------------------------------------------------------------");
            log.warn("[FundHub Local Development Notice]");
            log.warn("No service-account key file was detected in FIREBASE_SERVICE_ACCOUNT_PATH.");
            log.warn("Starting backend with local credentials for testing and health verification.");
            log.warn("For live database transactions, set your key path in PowerShell:");
            log.warn("  $env:FIREBASE_SERVICE_ACCOUNT_PATH=\"C:\\keys\\fundhub-key.json\"");
            log.warn("----------------------------------------------------------------------------------------");
            return GoogleCredentials.create(new AccessToken("fundhub-local-dev-token", new Date(System.currentTimeMillis() + 86400000L)));
        }

        // 7. Strict failure if fallback is disabled
        throw new IllegalStateException(
                "Firebase credentials missing. Set FIREBASE_SERVICE_ACCOUNT_PATH (local file) or FIREBASE_SERVICE_ACCOUNT_JSON (hosting).");
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
