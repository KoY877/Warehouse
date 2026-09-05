package io.github.koy877.warehouse.dto;

import java.time.Instant;

import io.github.koy877.warehouse.entities.User;
import io.github.koy877.warehouse.entities.enums.Role;

/**
 * API-Antwort fuer Users. Bewusst ohne firebaseUid: das Frontend kennt sie
 * bereits ueber das Firebase JS SDK, eine zusaetzliche Ausgabe ueber die
 * eigene API bringt keinen Mehrwert. Entities werden grundsaetzlich nie
 * direkt zurueckgegeben (siehe CLAUDE.md Security-Prinzipien).
 */
public record UserResponse(
        String id,
        String email,
        String displayName,
        Role role,
        Instant createdAt
) {

    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
