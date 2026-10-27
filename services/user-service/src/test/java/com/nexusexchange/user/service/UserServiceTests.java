package com.nexusexchange.user.service;

import com.nexusexchange.user.dto.AuthResponseDto;
import com.nexusexchange.user.dto.UserLoginDto;
import com.nexusexchange.user.dto.UserProfileDto;
import com.nexusexchange.user.dto.UserRegistrationDto;
import com.nexusexchange.user.entity.Role;
import com.nexusexchange.user.entity.User;
import com.nexusexchange.user.exception.InvalidCredentialsException;
import com.nexusexchange.user.exception.UserAlreadyExistsException;
import com.nexusexchange.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserServiceImpl. Uses a REAL BCrypt encoder (cost 4 to keep the tests fast)
 * so we test actual hashing behaviour, not a mock that always says "yes".
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    private static final String RAW_PASSWORD = "secure123";

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private UserServiceImpl userService;
    private UserRegistrationDto registrationDto;
    private UserLoginDto loginDto;
    private User storedUser;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(userRepository, passwordEncoder);

        registrationDto = UserRegistrationDto.builder()
                .username("john_doe")
                .email("john@example.com")
                .password(RAW_PASSWORD)
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("1234567890")
                .build();

        loginDto = UserLoginDto.builder()
                .usernameOrEmail("john_doe")
                .password(RAW_PASSWORD)
                .build();

        storedUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .passwordHash(passwordEncoder.encode(RAW_PASSWORD))
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("1234567890")
                .role(Role.ROLE_USER)
                .build();
    }

    private User captureSavedUser() {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        return captor.getValue();
    }

    private void allowRegistration() {
        when(userRepository.existsByUsernameIgnoreCase(any())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- register ----------

    @Test
    void registerUser_returnsProfile() {
        allowRegistration();

        UserProfileDto result = userService.registerUser(registrationDto);

        assertThat(result.getUsername()).isEqualTo("john_doe");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void registerUser_storesBcryptHash_notRawPassword() {
        allowRegistration();

        userService.registerUser(registrationDto);
        String hash = captureSavedUser().getPasswordHash();

        assertThat(hash)
                .isNotEqualTo(RAW_PASSWORD)
                .doesNotContain(RAW_PASSWORD)
                .matches("^\\$2[aby]\\$\\d{2}\\$.{53}$")   // pure BCrypt format, no prefix
                .hasSize(60);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, hash)).isTrue();
    }

    @Test
    void registerUser_samePasswordTwice_givesDifferentHashes() {
        allowRegistration();

        userService.registerUser(registrationDto);
        userService.registerUser(registrationDto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getPasswordHash())
                .isNotEqualTo(captor.getAllValues().get(1).getPasswordHash());   // random salt
    }

    @Test
    void registerUser_usernameExists_throws() {
        when(userRepository.existsByUsernameIgnoreCase(any())).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(registrationDto))
                .isInstanceOf(UserAlreadyExistsException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerUser_emailExists_throws() {
        when(userRepository.existsByUsernameIgnoreCase(any())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(any())).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(registrationDto))
                .isInstanceOf(UserAlreadyExistsException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    // ---------- login ----------

    @Test
    void loginUser_correctPassword_succeeds() {
        when(userRepository.findByUsernameIgnoreCase("john_doe")).thenReturn(Optional.of(storedUser));

        AuthResponseDto result = userService.loginUser(loginDto);

        assertThat(result.getToken()).isNotBlank();
        assertThat(result.getUser().getUsername()).isEqualTo("john_doe");
    }

    @Test
    void registerThenLogin_roundTrip_succeeds() {
        // Catches the bug where register stores something login can't verify (e.g. "SHA256:" + hash)
        allowRegistration();
        userService.registerUser(registrationDto);
        User saved = captureSavedUser();
        when(userRepository.findByUsernameIgnoreCase("john_doe")).thenReturn(Optional.of(saved));

        AuthResponseDto result = userService.loginUser(loginDto);

        assertThat(result.getUser().getUsername()).isEqualTo("john_doe");
    }

    @Test
    void loginUser_wrongPassword_throws() {
        when(userRepository.findByUsernameIgnoreCase("john_doe")).thenReturn(Optional.of(storedUser));
        loginDto.setPassword("wrongpassword");

        assertThatThrownBy(() -> userService.loginUser(loginDto))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginUser_unknownUser_throwsSameException() {
        // Same exception as wrong password, so attackers can't tell which usernames exist
        when(userRepository.findByUsernameIgnoreCase(any())).thenReturn(Optional.empty());
        lenient().when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loginUser(loginDto))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
