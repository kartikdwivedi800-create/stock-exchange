package com.nexus.user_service.service;

import com.nexus.user_service.dto.AuthResponseDto;
import com.nexus.user_service.dto.UserLoginDto;
import com.nexus.user_service.dto.UserRegistrationDto;
import com.nexus.user_service.dto.UserProfileDto;
import com.nexus.user_service.entity.Role;
import com.nexus.user_service.entity.User;
import com.nexus.user_service.exception.InvalidCredentialsException;
import com.nexus.user_service.exception.UserAlreadyExistsException;
import com.nexus.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTests {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegistrationDto registrationDto;
    private UserLoginDto loginDto;
    private User mockUser;

    @BeforeEach
    void setUp() {
        registrationDto = UserRegistrationDto.builder()
                .username("john_doe")
                .email("john@example.com")
                .password("secure123")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("1234567890")
                .build();

        loginDto = UserLoginDto.builder()
                .usernameOrEmail("john_doe")
                .password("secure123")
                .build();

        mockUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .passwordHash("SHA256:" + "secure123".hashCode())
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("1234567890")
                .role(Role.ROLE_USER)
                .build();
    }

    @Test
    void registerUser_Success() {
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        UserProfileDto result = userService.registerUser(registrationDto);

        assertNotNull(result);
        assertEquals("john_doe", result.getUsername());
        assertEquals("john@example.com", result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerUser_UsernameExists_ThrowsException() {
        when(userRepository.existsByUsername(any())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.registerUser(registrationDto));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginUser_Success() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(mockUser));

        AuthResponseDto result = userService.loginUser(loginDto);

        assertNotNull(result);
        assertNotNull(result.getToken());
        assertEquals("john_doe", result.getUser().getUsername());
    }

    @Test
    void loginUser_InvalidPassword_ThrowsException() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(mockUser));
        loginDto.setPassword("wrongpassword");

        assertThrows(InvalidCredentialsException.class, () -> userService.loginUser(loginDto));
    }
}
