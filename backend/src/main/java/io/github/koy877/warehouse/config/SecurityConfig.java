package io.github.koy877.warehouse.config;

import io.github.koy877.warehouse.security.FirebaseAuthenticationFilter;
import io.github.koy877.warehouse.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Kein /api/auth/login-Endpoint mehr noetig: der Login findet komplett
 * im Angular-Frontend ueber das Firebase JS SDK statt. Das Backend
 * verifiziert nur noch das mitgeschickte ID-Token
 * (FirebaseAuthenticationFilter).
 *
 * @EnableMethodSecurity aktiviert @PreAuthorize auf Controller-Ebene
 * (z. B. UserController) als Verteidigung in der Tiefe zusaetzlich zu den
 * HTTP-Level-Regeln unten.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final UserService userService;

    public SecurityConfig(UserService userService) {
        this.userService = userService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/users/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/locations/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/locations/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/stock_movements/**").hasRole("ADMIN")
                    .anyRequest().authenticated())
            .addFilterBefore(
                    new FirebaseAuthenticationFilter(userService),
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
