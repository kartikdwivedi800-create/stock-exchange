package com.nexusexchange.user;

import com.nexusexchange.common.dto.ErrorResponse;
import com.nexusexchange.testsupport.PostgresTestcontainersConfiguration;
import com.nexusexchange.user.dto.AuthResponseDto;
import com.nexusexchange.user.dto.ChangePasswordRequest;
import com.nexusexchange.user.dto.RefreshTokenRequest;
import com.nexusexchange.user.dto.UserLoginDto;
import com.nexusexchange.user.dto.UserProfileDto;
import com.nexusexchange.user.dto.UserRegistrationDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
@TestPropertySource(properties = {
        "nexus.admin.username=admin",
        "nexus.admin.password=AdminPass123!",
        "nexus.security.jwt.secret=integration-test-secret-that-is-at-least-32-chars-long!"
})
class AuthFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String uniqueUsername() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private UserProfileDto registerUser(String username, String password) {
        UserRegistrationDto registration = UserRegistrationDto.builder()
                .username(username)
                .email(username + "@example.com")
                .password(password)
                .firstName("Test")
                .lastName("User")
                .phoneNumber("+1234567890")
                .build();

        ResponseEntity<UserProfileDto> response = restTemplate.postForEntity(
                "/api/v1/auth/register", registration, UserProfileDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private AuthResponseDto loginUser(String usernameOrEmail, String password) {
        UserLoginDto loginDto = new UserLoginDto(usernameOrEmail, password);
        ResponseEntity<AuthResponseDto> response = restTemplate.postForEntity(
                "/api/v1/auth/login", loginDto, AuthResponseDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private AuthResponseDto registerAndLogin(String username, String password) {
        registerUser(username, password);
        return loginUser(username, password);
    }

    @Test
    @DisplayName("Scenario 1: register -> login -> GET /users/me with Bearer token -> 200 and right username")
    void scenario1_registerLoginAndGetMe_returnsProfileWithMatchingUsername() {
        String username = uniqueUsername();
        String password = "Password123!";

        registerUser(username, password);
        AuthResponseDto auth = loginUser(username, password);
        assertThat(auth.getAccessToken()).isNotBlank();
        assertThat(auth.getRefreshToken()).isNotBlank();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(auth.getAccessToken());
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<UserProfileDto> response = restTemplate.exchange(
                "/api/v1/users/me", HttpMethod.GET, request, UserProfileDto.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getUsername()).isEqualTo(username);
        assertThat(response.getBody().getEmail()).isEqualTo(username + "@example.com");
    }

    @Test
    @DisplayName("Scenario 2: GET /users/me without token -> 401 JSON; with tampered token -> 401")
    void scenario2_getMe_withoutTokenOrTamperedToken_returns401() {
        // Without token
        ResponseEntity<ErrorResponse> noTokenResponse = restTemplate.getForEntity(
                "/api/v1/users/me", ErrorResponse.class);
        assertThat(noTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(noTokenResponse.getHeaders().getContentType()).isNotNull();
        assertThat(noTokenResponse.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
        assertThat(noTokenResponse.getBody()).isNotNull();
        assertThat(noTokenResponse.getBody().getStatus()).isEqualTo(401);

        // With tampered token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalidPayload.tamperedSignature");
        HttpEntity<Void> tamperedRequest = new HttpEntity<>(headers);

        ResponseEntity<ErrorResponse> tamperedResponse = restTemplate.exchange(
                "/api/v1/users/me", HttpMethod.GET, tamperedRequest, ErrorResponse.class);
        assertThat(tamperedResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(tamperedResponse.getBody()).isNotNull();
        assertThat(tamperedResponse.getBody().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Scenario 3: refresh token rotation and reuse detection invalidates all tokens")
    void scenario3_refreshRotationAndReuseDetection_invalidatesAllTokens() {
        String username = uniqueUsername();
        String password = "Password123!";
        AuthResponseDto initialAuth = registerAndLogin(username, password);

        String oldAccessToken = initialAuth.getAccessToken();
        String oldRefreshToken = initialAuth.getRefreshToken();

        // Rotate token
        ResponseEntity<AuthResponseDto> refreshedResponse = restTemplate.postForEntity(
                "/api/v1/auth/refresh", new RefreshTokenRequest(oldRefreshToken), AuthResponseDto.class);
        assertThat(refreshedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        AuthResponseDto newAuth = refreshedResponse.getBody();
        assertThat(newAuth).isNotNull();
        assertThat(newAuth.getAccessToken()).isNotBlank().isNotEqualTo(oldAccessToken);
        assertThat(newAuth.getRefreshToken()).isNotBlank().isNotEqualTo(oldRefreshToken);

        String newRefreshToken = newAuth.getRefreshToken();

        // Old refresh token -> 401 (reuse detected -> all tokens of user revoked)
        ResponseEntity<ErrorResponse> replayOld = restTemplate.postForEntity(
                "/api/v1/auth/refresh", new RefreshTokenRequest(oldRefreshToken), ErrorResponse.class);
        assertThat(replayOld.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // NEW refresh token -> 401 too
        ResponseEntity<ErrorResponse> replayNew = restTemplate.postForEntity(
                "/api/v1/auth/refresh", new RefreshTokenRequest(newRefreshToken), ErrorResponse.class);
        assertThat(replayNew.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Scenario 4: logout -> refresh with token -> 401; logout again -> 204")
    void scenario4_logout_revokesToken_andSubsequentLogoutReturns204() {
        String username = uniqueUsername();
        String password = "Password123!";
        AuthResponseDto auth = registerAndLogin(username, password);
        String refreshToken = auth.getRefreshToken();

        // Logout
        ResponseEntity<Void> logout1 = restTemplate.postForEntity(
                "/api/v1/auth/logout", new RefreshTokenRequest(refreshToken), Void.class);
        assertThat(logout1.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Refresh with logged out token -> 401
        ResponseEntity<ErrorResponse> refreshAttempt = restTemplate.postForEntity(
                "/api/v1/auth/refresh", new RefreshTokenRequest(refreshToken), ErrorResponse.class);
        assertThat(refreshAttempt.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Logout again -> 204
        ResponseEntity<Void> logout2 = restTemplate.postForEntity(
                "/api/v1/auth/logout", new RefreshTokenRequest(refreshToken), Void.class);
        assertThat(logout2.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("Scenario 5: 5 wrong passwords -> 6th login with right password -> 423")
    void scenario5_repeatedFailedLogins_lockAccountWith423() {
        String username = uniqueUsername();
        String password = "CorrectPassword123!";
        registerUser(username, password);

        for (int i = 0; i < 5; i++) {
            ResponseEntity<ErrorResponse> wrong = restTemplate.postForEntity(
                    "/api/v1/auth/login", new UserLoginDto(username, "WrongPassword123!"), ErrorResponse.class);
            assertThat(wrong.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        // 6th attempt with CORRECT password -> 423 Locked
        ResponseEntity<ErrorResponse> lockedAttempt = restTemplate.postForEntity(
                "/api/v1/auth/login", new UserLoginDto(username, password), ErrorResponse.class);
        assertThat(lockedAttempt.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
        assertThat(lockedAttempt.getBody()).isNotNull();
        assertThat(lockedAttempt.getBody().getStatus()).isEqualTo(423);
    }

    @Test
    @DisplayName("Scenario 6: seeded admin -> GET /admin/users -> 200; normal user -> 403")
    void scenario6_seededAdminCanAccessAdminEndpoint_normalUserGets403() {
        // Admin access -> 200
        ResponseEntity<AuthResponseDto> adminLogin = restTemplate.postForEntity(
                "/api/v1/auth/login", new UserLoginDto("admin", "AdminPass123!"), AuthResponseDto.class);
        assertThat(adminLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminLogin.getBody()).isNotNull();

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminLogin.getBody().getAccessToken());
        ResponseEntity<String> adminAccess = restTemplate.exchange(
                "/api/v1/admin/users", HttpMethod.GET, new HttpEntity<>(adminHeaders), String.class);
        assertThat(adminAccess.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Normal user access -> 403
        String normalUser = uniqueUsername();
        AuthResponseDto normalAuth = registerAndLogin(normalUser, "NormalPass123!");

        HttpHeaders normalHeaders = new HttpHeaders();
        normalHeaders.setBearerAuth(normalAuth.getAccessToken());
        ResponseEntity<ErrorResponse> normalAccess = restTemplate.exchange(
                "/api/v1/admin/users", HttpMethod.GET, new HttpEntity<>(normalHeaders), ErrorResponse.class);
        assertThat(normalAccess.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(normalAccess.getBody()).isNotNull();
        assertThat(normalAccess.getBody().getStatus()).isEqualTo(403);
    }

    @Test
    @DisplayName("Scenario 7: change password -> old refresh token -> 401; old password -> 401; new password -> 200")
    void scenario7_changePassword_invalidatesRefreshTokensAndOldPassword() {
        String username = uniqueUsername();
        String oldPassword = "OldPassword123!";
        String newPassword = "NewPassword123!";

        AuthResponseDto auth = registerAndLogin(username, oldPassword);
        String accessToken = auth.getAccessToken();
        String oldRefreshToken = auth.getRefreshToken();

        // Change password
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ChangePasswordRequest changeRequest = new ChangePasswordRequest(oldPassword, newPassword);
        ResponseEntity<Void> changeResponse = restTemplate.exchange(
                "/api/v1/users/me/password", HttpMethod.PUT, new HttpEntity<>(changeRequest, headers), Void.class);
        assertThat(changeResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Old refresh token -> 401
        ResponseEntity<ErrorResponse> oldRefresh = restTemplate.postForEntity(
                "/api/v1/auth/refresh", new RefreshTokenRequest(oldRefreshToken), ErrorResponse.class);
        assertThat(oldRefresh.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Login with old password -> 401
        ResponseEntity<ErrorResponse> oldLogin = restTemplate.postForEntity(
                "/api/v1/auth/login", new UserLoginDto(username, oldPassword), ErrorResponse.class);
        assertThat(oldLogin.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Login with new password -> 200
        ResponseEntity<AuthResponseDto> newLogin = restTemplate.postForEntity(
                "/api/v1/auth/login", new UserLoginDto(username, newPassword), AuthResponseDto.class);
        assertThat(newLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(newLogin.getBody()).isNotNull();
        assertThat(newLogin.getBody().getAccessToken()).isNotBlank();
    }
}

