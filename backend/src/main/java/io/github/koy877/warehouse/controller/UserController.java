package io.github.koy877.warehouse.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.koy877.warehouse.dto.UpdateRoleRequest;
import io.github.koy877.warehouse.dto.UserResponse;
import io.github.koy877.warehouse.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Alle Endpoints hier sind zusaetzlich bereits ueber SecurityConfig
 * ("/api/users/**" -> hasRole("ADMIN")) auf HTTP-Ebene abgesichert.
 * @PreAuthorize hier ist bewusste Verteidigung in der Tiefe, nicht die
 * einzige Absicherung.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<UserResponse> findAll() {
        return userService.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable String id) {
        return userService.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable String id, @Valid @RequestBody UpdateRoleRequest request) {
        return userService.changeRole(id, request.role());
    }
}
