package com.nexusexchange.user.controller;

import com.nexusexchange.user.config.JsonAccessDeniedHandler;
import com.nexusexchange.user.config.JsonAuthenticationEntryPoint;
import com.nexusexchange.user.config.SecurityConfig;
import com.nexusexchange.user.exception.InvalidCredentialsException;
import com.nexusexchange.user.exception.UserAlreadyExistsException;
import com.nexusexchange.user.exception.InvalidRefreshTokenException;
import com.nexusexchange.user.service.RefreshTokenService;
import com.nexusexchange.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import com.nexusexchange.user.dto.AuthResponseDto;
import com.nexusexchange.user.dto.UserProfileDto;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the /api/v1 routes and that the shared GlobalExceptionHandler from common-library
 * is picked up automatically (no handler exists in user-service itself).
 */
@WebMvcTest(controllers = {AuthController.class, UserController.class})
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class})
class AuthControllerTest {

    private static final String VALID_REGISTRATION = """
            {"username":"kartik","email":"kartik@example.com","password":"Secret123!",
             "firstName":"Kartik","lastName":"D","phoneNumber":"9999999999"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserService userService;

    /** Needed by oauth2ResourceServer(jwt); the real decoder is covered by JwtConfigTest. */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @Test
    void loginReturnsAccessTokenResponseShape() throws Exception {
        when(userService.loginUser(any())).thenReturn(AuthResponseDto.builder()
                .accessToken("header.payload.signature")
                .expiresIn(900)
                .user(UserProfileDto.builder().id(1L).username("kartik").build())
                .build());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"kartik\",\"password\":\"Secret123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("header.payload.signature"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.user.username").value("kartik"));
    }

    @Test
    void validJwtOnAuthenticatedEndpointIsNot401() throws Exception {
        mvc.perform(get("/api/v1/users/me").with(jwt().jwt(j -> j.subject("42"))))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void invalidBearerTokenReturns401Json() throws Exception {
        when(jwtDecoder.decode(any())).thenThrow(new BadJwtException("bad token"));

        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer not-a-valid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/v1/users/me"));
    }

    @Test
    void userRoleOnAdminEndpointReturns403Json() throws Exception {
        mvc.perform(get("/api/v1/admin/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void duplicateUserReturns409FromSharedHandler() throws Exception {
        when(userService.registerUser(any())).thenThrow(new UserAlreadyExistsException("Username is already taken"));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTRATION))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Username is already taken"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"));
    }

    @Test
    void invalidRegistrationListsFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        when(userService.loginUser(any())).thenThrow(new InvalidCredentialsException("Invalid username/email or password"));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"kartik\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profileWithoutTokenReturns401Json() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").exists())
                .andExpect(jsonPath("$.requestId").exists());
    }

    @Test
    void responseNeverSetsSessionCookie() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getCookie("JSESSIONID")).isNull())
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getHeaders("Set-Cookie")).isEmpty());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTRATION))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getCookie("JSESSIONID")).isNull());
    }

    @Test
    void registerIsPublic() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTRATION))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void adminEndpointWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oldUnprefixedRouteIsGone() throws Exception {
        // Authenticated, so security lets the request through and the missing mapping shows as 404
        mvc.perform(post("/register").with(user("someone")).contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTRATION))
                .andExpect(status().isNotFound());
    }

    @Test
    void passwordShorterThan8Returns400() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registrationWithPassword("Short1!")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").exists());
        verify(userService, never()).registerUser(any());
    }

    @Test
    void passwordLongerThan72Returns400() throws Exception {
        // BCrypt silently ignores bytes after 72, so we reject longer passwords up front
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registrationWithPassword("a".repeat(73))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'password')]").exists());
        verify(userService, never()).registerUser(any());
    }

    @Test
    void passwordOfExactly8IsAccepted() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registrationWithPassword("Exactly8")))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void authEndpointsAreOpenWithoutLogin() throws Exception {
        // Public auth routes: no 401 and no Basic-auth challenge header
        when(userService.loginUser(any())).thenThrow(new InvalidCredentialsException("Invalid username/email or password"));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"usernameOrEmail\":\"kartik\",\"password\":\"Secret123!\"}"))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getHeader("WWW-Authenticate")).isNull());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTRATION))
                .andExpect(status().is2xxSuccessful());
    }

    // ---------- refresh / logout (NX-303) ----------

    private static final String REFRESH_BODY = "{\"refreshToken\":\"some-raw-token\"}";

    @Test
    void refreshReturns200WithNewPair() throws Exception {
        when(refreshTokenService.refresh("some-raw-token")).thenReturn(AuthResponseDto.builder()
                .accessToken("new.access.token")
                .expiresIn(900)
                .refreshToken("new-raw-refresh")
                .user(UserProfileDto.builder().id(1L).username("kartik").build())
                .build());

        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(REFRESH_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.access.token"))
                .andExpect(jsonPath("$.refreshToken").value("new-raw-refresh"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void refreshWithInvalidTokenReturns401Json() throws Exception {
        when(refreshTokenService.refresh(any())).thenThrow(new InvalidRefreshTokenException());

        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(REFRESH_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/refresh"));
    }

    @Test
    void refreshWithBlankTokenReturns400() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'refreshToken')]").exists());
        verify(refreshTokenService, never()).refresh(any());
    }

    @Test
    void logoutReturns204AndCallsService() throws Exception {
        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON).content(REFRESH_BODY))
                .andExpect(status().isNoContent());
        verify(refreshTokenService).logout("some-raw-token");
    }

    @Test
    void logoutWithBlankTokenReturns400() throws Exception {
        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verify(refreshTokenService, never()).logout(any());
    }

    private static String registrationWithPassword(String password) {
        return """
                {"username":"kartik","email":"kartik@example.com","password":"%s",
                 "firstName":"Kartik","lastName":"D","phoneNumber":"9999999999"}
                """.formatted(password);
    }
}
