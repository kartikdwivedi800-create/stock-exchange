package com.nexus.user_service.service;

import com.nexus.user_service.dto.AuthResponseDto;
import com.nexus.user_service.dto.UserLoginDto;
import com.nexus.user_service.dto.UserRegistrationDto;
import com.nexus.user_service.dto.UserProfileDto;
import com.nexus.user_service.entity.Role;
import com.nexus.user_service.entity.User;
import com.nexus.user_service.exception.InvalidCredentialsException;
import com.nexus.user_service.exception.ResourceNotFoundException;
import com.nexus.user_service.exception.UserAlreadyExistsException;
import com.nexus.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserProfileDto registerUser(UserRegistrationDto registrationDto) {
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new UserAlreadyExistsException("Username is already taken");
        }
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        // Mock hashing for the skeleton stage (before Spring Security is fully integrated)
        String mockPasswordHash = "SHA256:" + registrationDto.getPassword().hashCode();

        User user = User.builder()
                .username(registrationDto.getUsername())
                .email(registrationDto.getEmail())
                .passwordHash(mockPasswordHash)
                .firstName(registrationDto.getFirstName())
                .lastName(registrationDto.getLastName())
                .phoneNumber(registrationDto.getPhoneNumber())
                .role(Role.ROLE_USER)
                .build();

        User savedUser = userRepository.save(user);
        return mapToProfileDto(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDto loginUser(UserLoginDto loginDto) {
        Optional<User> userOpt = userRepository.findByUsername(loginDto.getUsernameOrEmail());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(loginDto.getUsernameOrEmail());
        }

        if (userOpt.isEmpty()) {
            throw new InvalidCredentialsException("Invalid username/email or password");
        }

        User user = userOpt.get();
        String expectedHash = "SHA256:" + loginDto.getPassword().hashCode();
        if (!user.getPasswordHash().equals(expectedHash)) {
            throw new InvalidCredentialsException("Invalid username/email or password");
        }

        // Generate a mock JWT for the skeleton
        String mockJwtToken = "mock-jwt-token-for-" + user.getUsername() + "-" + UUID.randomUUID();

        return AuthResponseDto.builder()
                .token(mockJwtToken)
                .user(mapToProfileDto(user))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToProfileDto(user);
    }

    @Override
    @Transactional
    public UserProfileDto updateUserProfile(Long id, UserProfileDto profileDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Update profile fields
        user.setFirstName(profileDto.getFirstName());
        user.setLastName(profileDto.getLastName());
        user.setPhoneNumber(profileDto.getPhoneNumber());

        User updatedUser = userRepository.save(user);
        return mapToProfileDto(updatedUser);
    }

    private UserProfileDto mapToProfileDto(User user) {
        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
