package io.github.koy877.warehouse.entities;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import io.github.koy877.warehouse.entities.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "Users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    /**
     * Die von Firebase vergebene, eindeutige User-ID (aus dem verifizierten
     * ID-Token). Referenz zur Firebase-Identitaet, nicht die interne id.
     */
    @Column(name = "firebase_uid", nullable = false, unique = true, updatable = false)
    private String firebaseUid;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Setter(AccessLevel.PRIVATE)
    private Role role;

    @Column(name = "user_order", nullable = false)
    private int order;

    @CreationTimestamp
    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name="updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Wird beim Auto-Provisioning eines neuen Firebase-Users ohne
     * Admin-Bootstrap-Treffer verwendet (siehe FirebaseAuthenticationFilter).
     * Rolle wird bewusst serverseitig auf LAGERIST gesetzt und nie aus einem
     * Request-DTO uebernommen.
     */
    public User(String firebaseUid, String email, String displayName) {
        this(firebaseUid, email, displayName, Role.LAGERIST);
    }

    /**
     * Nur fuer den Admin-Bootstrap gedacht (siehe
     * UserService.loadOrProvisionUser): die Rolle stammt aus dem
     * serverseitigen Vergleich der Token-E-Mail mit ADMIN_BOOTSTRAP_EMAIL,
     * nie aus einem Request-DTO - daher kein Verstoss gegen "kein
     * oeffentliches setRole()".
     */
    public User(String firebaseUid, String email, String displayName, Role role) {
        this.firebaseUid = firebaseUid;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
    }

    /**
     * Einzig zulaessiger Weg, die Rolle nach der Erstanlage zu aendern.
     * Bewusst als benannte Methode statt oeffentlichem Setter, damit ein
     * Rollenwechsel immer eine bewusste, im Service gepruefte Aktion ist
     * (siehe UserService.changeRole) und nicht versehentlich per
     * Mass-Assignment aus einem DTO uebernommen werden kann.
     */
    public void changeRole(Role newRole) {
        this.role = newRole;
    }

}
