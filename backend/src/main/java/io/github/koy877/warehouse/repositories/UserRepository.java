package io.github.koy877.warehouse.repositories;
 
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.koy877.warehouse.entities.User;
import io.github.koy877.warehouse.entities.enums.Role;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByFirebaseUid(String firebaseUid);

    // Wird fuer die "letzter Admin darf nicht degradiert werden"-Regel in
    // UserService.changeRole benoetigt.
    long countByRole(Role role);
}
 
 
