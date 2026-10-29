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
import com.nexusexchange.user.config.SecurityProperties;
import com.nexusexchange.user.exception.AccountLockedException;
import com.nexusexchange.user.exception.AccountNotActiveException;
import com.nexusexchange.common.enums.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private static final String INVALID_CREDENTIALS = "Invalid username/email or password";

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final TokenService tokenService;

    private final RefreshTokenService refreshTokenService;

    private final SecurityProperties securityProperties;

    private final Clock clock;

    /** BCrypt hash (same cost as real ones) checked when the username is unknown. */
    private final String dummyPasswordHash;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           TokenService tokenService, RefreshTokenService refreshTokenService,
                           SecurityProperties securityProperties, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.refreshTokenService = refreshTokenService;
        this.securityProperties = securityProperties;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode("nexus-dummy-password-never-matches");
    }

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

    /**
     * Not readOnly (it writes the failed-attempt counter and a refresh token). noRollbackFor: the counter
     * update must be committed even though the login fails with InvalidCredentialsException.
     */
    @Override
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public AuthResponseDto loginUser(UserLoginDto loginDto) {
        Instant now = clock.instant();
        Optional<User> userOpt = userRepository.findByUsernameIgnoreCase(loginDto.getUsernameOrEmail());
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmailIgnoreCase(loginDto.getUsernameOrEmail());
        }

        if (userOpt.isEmpty()) {
            // Same BCrypt work as a real check, so response time does not reveal unknown usernames
            passwordEncoder.matches(loginDto.getPassword(), dummyPasswordHash);
            throw new InvalidCredentialsException(INVALID_CREDENTIALS);
        }

        User user = userOpt.get();

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new AccountLockedException();
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountNotActiveException();
        }

        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPasswordHash())) {
            registerFailedAttempt(user, now);
            throw new InvalidCredentialsException(INVALID_CREDENTIALS);
        }

        if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        return AuthResponseDto.builder()
                .accessToken(tokenService.createAccessToken(user))
                .expiresIn(tokenService.accessTokenExpiresInSeconds())
                .refreshToken(refreshTokenService.issue(user))
                .user(mapToProfileDto(user))
                .build();
    }

    private void registerFailedAttempt(User user, Instant now) {
        SecurityProperties.Login login = securityProperties.login();
        int attempts = user.getFailedLoginAttempts() + 1;
        if (attempts >= login.maxFailedAttempts()) {
            user.setLockedUntil(now.plus(login.lockDuration()));
            attempts = 0;
        }
        user.setFailedLoginAttempts(attempts);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDto getUserProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapToProfileDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<UserProfileDto> listUsers(org.springframework.data.domain.Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToProfileDto);
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
