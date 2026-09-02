package io.github.koy877.warehouse.config;

import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;

/**
 * Initialisiert die FirebaseApp beim Start mit den Service-Account-Credentials.
 * Der Pfad zur JSON-Datei kommt aus der Umgebungsvariable/Property
 * (siehe application.properties), die Datei selbst ist gitignored -
 * analog zur .env-Konvention im Helpdesk-Projekt.
 */

@Profile("!test")
@Configuration
public class FirebaseConfig {
    @Value("${FIREBASE_SERVICE_ACCOUNT_PATH}")
    private String serviceAccountPath;

    @PostConstruct 
    public void initialize () {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(new FileInputStream(serviceAccountPath)))
                    .build();
                        
                FirebaseApp.initializeApp(options);
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                "Firebase konnte nicht initialisiert werden. Pfad pruefen: " + serviceAccountPath, e);
        }
    }
}
