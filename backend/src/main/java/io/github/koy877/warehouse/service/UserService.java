package io.github.koy877.warehouse.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.firebase.auth.FirebaseToken;

import io.github.koy877.warehouse.dto.UserResponse;
import io.github.koy877.warehouse.entities.User;
import io.github.koy877.warehouse.entities.enums.Role;
import io.github.koy877.warehouse.exception.ConflictException;
import io.github.koy877.warehouse.exception.ResourceNotFoundException;
import io.github.koy877.warehouse.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Loest das Henne-Ei-Problem beim ersten ADMIN: PATCH /role setzt selbst
     * schon ADMIN voraus, also gibt es sonst keinen Weg ueber die API, den
     * allerersten Admin anzulegen. Default leerer String (kein Bootstrap
     * aktiv) statt null, damit der Vergleich unten ohne Null-Check auskommt -
     * auch unter Mockito @InjectMocks, wo @Value nicht ausgewertet wird.
     */
    @Value("${ADMIN_BOOTSTRAP_EMAIL:}")
    private String adminBootstrapEmail = "";

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse findById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User nicht gefunden: " + id));
        return UserResponse.fromEntity(user);
    }

    /**
     * Aendert die Rolle eines Users. Die "letzter Admin"-Regel deckt zwei
     * Faelle in einer einzigen Pruefung ab: ein Admin degradiert sich
     * selbst, oder zwei Admins degradieren sich gegenseitig bis keiner
     * mehr uebrig ist. Ein Vergleich mit der aufrufenden User-Id waere
     * dafuer nicht ausreichend (siehe Review-Diskussion).
     */
    @Transactional
    public UserResponse changeRole(String userId, Role newRole) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User nicht gefunden: " + userId));

        boolean waereLetzterAdmin = target.getRole() == Role.ADMIN
                && newRole != Role.ADMIN
                && userRepository.countByRole(Role.ADMIN) <= 1;

        if (waereLetzterAdmin) {
            throw new ConflictException("Der letzte verbleibende ADMIN kann nicht degradiert werden.");
        }

        target.changeRole(newRole);
        return UserResponse.fromEntity(userRepository.save(target));
    }

    /**
     * Laedt den zum Firebase-Token gehoerenden User, oder legt ihn bei
     * Erstlogin an. Verschoben aus dem FirebaseAuthenticationFilter, damit
     * die Provisioning-Logik (Default-Rolle LAGERIST) isoliert testbar
     * bleibt und der Filter nicht direkt am Repository vorbei die
     * Architekturschicht "service" ueberspringt.
     */
    @Transactional
    public User loadOrProvisionUser(FirebaseToken decodedToken) {
        return userRepository.findByFirebaseUid(decodedToken.getUid())
            .orElseGet(() -> {
                String email = decodedToken.getEmail();
                String displayName = decodedToken.getName() != null ? decodedToken.getName() : email;
                Role role = !adminBootstrapEmail.isBlank() && adminBootstrapEmail.equalsIgnoreCase(email)
                        ? Role.ADMIN
                        : Role.LAGERIST;

                return userRepository.save(new User(decodedToken.getUid(), email, displayName, role));
            });
    }
}
