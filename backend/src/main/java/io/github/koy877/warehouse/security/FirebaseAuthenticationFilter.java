package io.github.koy877.warehouse.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import io.github.koy877.warehouse.entities.User;
import io.github.koy877.warehouse.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Verifiziert das Firebase-ID-Token aus dem Authorization-Header, laedt
 * (oder legt bei Erstlogin an) den zugehoerigen App-User und setzt die
 * Authentication im SecurityContext. Die Rolle stammt aus der eigenen DB,
 * nicht aus Firebase Custom Claims (Entscheidung siehe CLAUDE.md).
 *
 * Bekannte Einschraenkung (MVP): das Auto-Provisioning ist nicht gegen
 * parallele Erstlogins desselben Users abgesichert (kein expliziter Lock /
 * kein Abfangen der Unique-Constraint-Verletzung). Fuer den aktuellen
 * Umfang ausreichend, vor Produktivbetrieb nachschaerfen.
 */
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private final UserService userService;

    public FirebaseAuthenticationFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String idToken = authHeader.substring(7);

        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            User user = userService.loadOrProvisionUser(decodedToken);

            List<GrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
            );

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (FirebaseAuthException e) {
            // Ungueltiges/abgelaufenes Token -> keine Authentication setzen.
            // SecurityConfig weist den Request danach mit 401 zurueck.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}