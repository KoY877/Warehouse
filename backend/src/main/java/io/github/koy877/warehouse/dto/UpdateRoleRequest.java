package io.github.koy877.warehouse.dto;

import io.github.koy877.warehouse.entities.enums.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Request-DTO fuer PATCH /api/users/{id}/role. Bewusst das einzige Feld,
 * das ueber diesen Endpoint aenderbar ist - kein Mass-Assignment auf
 * andere User-Felder moeglich.
 */
public record UpdateRoleRequest(

        @NotNull(message = "Rolle darf nicht null sein")
        Role role
) {
}
