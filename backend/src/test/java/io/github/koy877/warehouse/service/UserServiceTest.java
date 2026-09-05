package io.github.koy877.warehouse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.firebase.auth.FirebaseToken;

import io.github.koy877.warehouse.dto.UserResponse;
import io.github.koy877.warehouse.entities.User;
import io.github.koy877.warehouse.entities.enums.Role;
import io.github.koy877.warehouse.exception.ConflictException;
import io.github.koy877.warehouse.exception.ResourceNotFoundException;
import io.github.koy877.warehouse.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User admin;
    private User lagerist;

    @BeforeEach
    void setUp() {
        admin = new User("firebase-admin-uid", "admin@warehouse.io", "Admin User");
        admin.setId("admin-id");
        admin.changeRole(Role.ADMIN);

        lagerist = new User("firebase-lagerist-uid", "lagerist@warehouse.io", "Lagerist User");
        lagerist.setId("lagerist-id");
    }

    @Test
    void findAll_gibtAlleUserAlsResponseZurueck() {
        when(userRepository.findAll()).thenReturn(List.of(admin, lagerist));

        List<UserResponse> result = userService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(UserResponse::id)
                .containsExactly(admin.getId(), lagerist.getId());
    }

    @Test
    void findById_wirftResourceNotFoundException_wennUserNichtExistiert() {
        when(userRepository.findById("unbekannt")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById("unbekannt"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findById_gibtUserResponseZurueck_wennUserExistiert() {
        when(userRepository.findById(lagerist.getId())).thenReturn(Optional.of(lagerist));

        UserResponse result = userService.findById(lagerist.getId());

        assertThat(result.id()).isEqualTo(lagerist.getId());
        assertThat(result.role()).isEqualTo(Role.WAREHOUSE_OPERATOR);
    }

    @Test
    void changeRole_erlaubtDegradierung_wennMehrereAdminsExistieren() {
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(2L);
        when(userRepository.save(admin)).thenReturn(admin);

        UserResponse result = userService.changeRole(admin.getId(), Role.WAREHOUSE_OPERATOR);

        assertThat(result.role()).isEqualTo(Role.WAREHOUSE_OPERATOR);
        verify(userRepository).save(admin);
    }

    @Test
    void changeRole_wirftConflictException_wennLetzterAdminDegradiertWuerde() {
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> userService.changeRole(admin.getId(), Role.WAREHOUSE_OPERATOR))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void changeRole_erlaubtBeliebigeAenderung_wennZielrolleAdminBleibt() {
        when(userRepository.findById(lagerist.getId())).thenReturn(Optional.of(lagerist));
        when(userRepository.save(lagerist)).thenReturn(lagerist);

        UserResponse result = userService.changeRole(lagerist.getId(), Role.ADMIN);

        assertThat(result.role()).isEqualTo(Role.ADMIN);
        verify(userRepository, never()).countByRole(any());
    }

    @Test
    void changeRole_wirftResourceNotFoundException_wennZielUserNichtExistiert() {
        when(userRepository.findById("unbekannt")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.changeRole("unbekannt", Role.ADMIN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void loadOrProvisionUser_gibtBestehendenUserZurueck_wennFirebaseUidBekannt() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn(lagerist.getFirebaseUid());
        when(userRepository.findByFirebaseUid(lagerist.getFirebaseUid())).thenReturn(Optional.of(lagerist));

        User result = userService.loadOrProvisionUser(token);

        assertThat(result).isEqualTo(lagerist);
        verify(userRepository, never()).save(any());
    }

    @Test
    void loadOrProvisionUser_legtNeuenUserAnMitRolleLagerist_wennFirebaseUidUnbekannt() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("neue-firebase-uid");
        when(token.getEmail()).thenReturn("neu@warehouse.io");
        when(token.getName()).thenReturn("Neuer User");
        when(userRepository.findByFirebaseUid("neue-firebase-uid")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.loadOrProvisionUser(token);

        assertThat(result.getFirebaseUid()).isEqualTo("neue-firebase-uid");
        assertThat(result.getDisplayName()).isEqualTo("Neuer User");
        assertThat(result.getRole()).isEqualTo(Role.WAREHOUSE_OPERATOR);
    }

    @Test
    void loadOrProvisionUser_verwendetEmailAlsDisplayNameFallback_wennFirebaseNameFehlt() {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("neue-firebase-uid");
        when(token.getEmail()).thenReturn("neu@warehouse.io");
        when(token.getName()).thenReturn(null);
        when(userRepository.findByFirebaseUid("neue-firebase-uid")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.loadOrProvisionUser(token);

        assertThat(result.getDisplayName()).isEqualTo("neu@warehouse.io");
    }

    @Test
    void loadOrProvisionUser_legtErstenAdminAn_wennEmailAdminBootstrapEmailEntspricht() {
        ReflectionTestUtils.setField(userService, "adminBootstrapEmail", "boss@warehouse.io");

        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("neue-firebase-uid");
        when(token.getEmail()).thenReturn("Boss@Warehouse.io");
        when(token.getName()).thenReturn("Der Chef");
        when(userRepository.findByFirebaseUid("neue-firebase-uid")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.loadOrProvisionUser(token);

        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void loadOrProvisionUser_legtNeuenUserAnMitRolleLagerist_wennAdminBootstrapEmailNichtGesetzt() {
        ReflectionTestUtils.setField(userService, "adminBootstrapEmail", "");

        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("neue-firebase-uid");
        when(token.getEmail()).thenReturn("neu@warehouse.io");
        when(token.getName()).thenReturn("Neuer User");
        when(userRepository.findByFirebaseUid("neue-firebase-uid")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.loadOrProvisionUser(token);

        assertThat(result.getRole()).isEqualTo(Role.WAREHOUSE_OPERATOR);
    }
}
