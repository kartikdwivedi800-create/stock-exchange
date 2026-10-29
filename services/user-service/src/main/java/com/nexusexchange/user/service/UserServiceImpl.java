package com.nexusexchange.user.service;

import com.nexusexchange.user.dto.AuthResponseDto;
import com.nexusexchange.user.dto.UpdateProfileRequest;
import com.nexusexchange.user.dto.UserLoginDto;
import com.nexusexchange.user.dto.UserRegistrationDto;
import com.nexusexchange.user.dto.UserProfileDto;
import com.nexusexchange.user.entity.Role;
import com.nexusexchange.user.entity.User;
import com.nexusexchange.user.exception.InvalidCredentialsException;
import com.nexusexchange.common.exception.ResourceNotFoundException;
import com.nexusexchange.user.exception.UserAlreadyExistsException;
import com.nexusexchange.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final TokenService tokenService;

    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public UserProfileDto registerUser(UserRegistrationDto registrationDto) {
        if (userRepository.existsByUsernameIgnoreCase(registrationDto.getUsername())) {
            throw new UserAlreadyExistsException("Username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(registrationDto.getEmail())) {
            throw new UserAlreadyExistsException("Email is already registered");
        }

        String passwordHash = passwordEncoder.encode(registrationDto.getPassword());

        User user = User.builder()
                .username(registrationDto.getUsername())
                .email(registrationDto.getEmail())
                .passwordHash(passwordHash)
                .firstName(registrationDto.getFirstName())
                .lastName(registrationDto.getLastName())
                .phoneNumber(registrationDto.getPhoneNumber())
                .role(Role.ROLE_USER)
                .build();

        User savedUser = userRepository.save(user);
        return mapToProfileDto(savedUser);
    }

    @Override
    // Not readOnly: login now inserts a refresh token row
    @Transactional
    public AuthResponseDto loginUser(UserLoginDto loginDto) {
        Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(loginDto.getUsernameOrEmail());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmailIgnoreCase(loginDto.getUsernameOrEmail());
        }

        if (userOpt.isEmpty()) {
            throw new InvalidCredentialsException("Invalid username/email or password");
        }

        User user = userOpt.get();

        if(!passwordEncoder.matches(loginDto.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid username/email or password");
        }
        return AuthResponseDto.builder()
                .accessToken(tokenService.createAccessToken(user))
                .expiresIn(tokenService.accessTokenExpiresInSeconds())
                .refreshToken(refreshTokenService.issue(user))
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
    public UserProfileDto updateProfile(Long id, UpdateProfileRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Only these three fields are editable by the user
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhoneNumber(request.phoneNumber());

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
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
