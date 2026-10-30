package com.nexusexchange.user.service;

import com.nexusexchange.common.enums.UserStatus;
import com.nexusexchange.common.exception.ForbiddenException;
import com.nexusexchange.user.config.JwtTestSupport;
import com.nexusexchange.user.dto.UserLoginDto;
import com.nexusexchange.user.entity.Role;
import com.nexusexchange.user.entity.User;
import com.nexusexchange.user.exception.AccountLockedException;
import com.nexusexchange.user.exception.AccountNotActiveException;
import com.nexusexchange.user.exception.InvalidCredentialsException;
import com.nexusexchange.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/** NX-308: failed-attempt counting, lockout, unlock, inactive accounts. Time is moved with a mutable clock. */
@ExtendWith(MockitoExtension.class)
class LoginLockoutTest {

    private static final Instant START = Instant.parse("2026-01-01T10:00:00Z");
    private static final String PASSWORD = "secure123";
    private static final Duration LOCK = Duration.ofMinutes(15);

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    private final PasswordEncoder encoder = spy(new BCryptPasswordEncoder(4));

    private MutableClock clock;
    private UserServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(START);
        TokenService tokenService = new TokenService(JwtTestSupport.encoder(), JwtTestSupport.properties(), clock);
        service = new UserServiceImpl(userRepository, encoder, tokenService, refreshTokenService,
                JwtTestSupport.properties(), clock);

        user = User.builder().id(1L).username("john").email("john@example.com")
                .passwordHash(encoder.encode(PASSWORD)).firstName("John").lastName("Doe")
                .role(Role.ROLE_USER).status(UserStatus.ACTIVE).build();
        lenient().when(userRepository.findByUsernameIgnoreCase("john")).thenReturn(Optional.of(user));
        lenient().when(refreshTokenService.issue(user)).thenReturn("raw-refresh");
    }

    private void login(String password) {
        service.loginUser(UserLoginDto.builder().usernameOrEmail("john").password(password).build());
    }

    private void failLogin() {
        assertThatThrownBy(() -> login("wrong-password")).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void fiveWrongPasswords_thenSixthWithRightPasswordIsLocked() {
        for (int i = 1; i <= 4; i++) {
            failLogin();
            assertThat(user.getFailedLoginAttempts()).isEqualTo(i);
            assertThat(user.getLockedUntil()).isNull();
        }
        failLogin(); // 5th failure locks the account and resets the counter

        assertThat(user.getLockedUntil()).isEqualTo(START.plus(LOCK));
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThatThrownBy(() -> login(PASSWORD)).isInstanceOf(AccountLockedException.class);
    }

    @Test
    void lockedAccountWithWrongPasswordStaysLockedAndCounterUnchanged() {
        user.setLockedUntil(START.plus(LOCK));

        assertThatThrownBy(() -> login("wrong-password")).isInstanceOf(AccountLockedException.class);

        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    void afterLockDurationRightPasswordWorksAgainAndStateIsReset() {
        for (int i = 0; i < 5; i++) {
            failLogin();
        }
        clock.set(START.plus(LOCK).minusSeconds(1));
        assertThatThrownBy(() -> login(PASSWORD)).isInstanceOf(AccountLockedException.class);

        clock.set(START.plus(LOCK)); // lock expires exactly at locked_until
        login(PASSWORD);

        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    void fourWrong_oneRight_fourWrong_isNotLocked() {
        for (int i = 0; i < 4; i++) {
            failLogin();
        }
        login(PASSWORD);
        assertThat(user.getFailedLoginAttempts()).isZero();

        for (int i = 0; i < 4; i++) {
            failLogin();
        }

        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(4);
        login(PASSWORD); // still allowed
    }

    @Test
    void inactiveUserGets403EvenWithRightPassword() {
        for (UserStatus status : new UserStatus[]{UserStatus.SUSPENDED, UserStatus.INACTIVE, UserStatus.PENDING}) {
            user.setStatus(status);

            assertThatThrownBy(() -> login(PASSWORD))
                    .isInstanceOf(AccountNotActiveException.class)
                    .isInstanceOf(ForbiddenException.class);
        }
        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    void unknownUserStillRunsPasswordCheckAndReturnsSame401() {
        lenient().when(userRepository.findByUsernameIgnoreCase("ghost")).thenReturn(Optional.empty());
        lenient().when(userRepository.findByEmailIgnoreCase("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loginUser(
                UserLoginDto.builder().usernameOrEmail("ghost").password("whatever1").build()))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username/email or password");

        verify(encoder).matches(eq("whatever1"), anyString());
    }

    /** A clock whose time the test can move. */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void set(Instant instant) {
            this.now = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
