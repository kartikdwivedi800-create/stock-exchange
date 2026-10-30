package com.nexusexchange.user.controller;

import com.nexusexchange.user.config.JsonAccessDeniedHandler;
import com.nexusexchange.user.config.JsonAuthenticationEntryPoint;
import com.nexusexchange.user.config.SecurityConfig;
import com.nexusexchange.user.dto.UpdateProfileRequest;
import com.nexusexchange.user.dto.UserProfileDto;
import com.nexusexchange.user.entity.Role;
import com.nexusexchange.user.service.UserService;
import com.nexusexchange.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** NX-305: the user id comes from the verified JWT, never from a client header. */
@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class})
class UserControllerTest {

    private static final String URL = "/api/v1/users/me";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor token() {
        return jwt().jwt(j -> j.subject("42").claim("role", "ROLE_USER"));
    }

    private static UserProfileDto profile(long id) {
        return UserProfileDto.builder().id(id).username("kartik").firstName("Kartik").lastName("D")
                .role(Role.ROLE_USER).build();
    }

    @Test
    void getMeCallsServiceWithIdFromToken() throws Exception {
        when(userService.getUserProfile(42L)).thenReturn(profile(42L));

        mvc.perform(get(URL).with(token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));

        verify(userService).getUserProfile(42L);
    }

    @Test
    void forgedUserIdHeaderIsIgnored() throws Exception {
        when(userService.getUserProfile(42L)).thenReturn(profile(42L));

        mvc.perform(get(URL).with(token()).header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));

        verify(userService).getUserProfile(42L);
        verify(userService, never()).getUserProfile(1L);
    }

    @Test
    void getMeForDeletedUserIs404() throws Exception {
        when(userService.getUserProfile(42L)).thenThrow(new ResourceNotFoundException("User not found with id: 42"));

        mvc.perform(get(URL).with(token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMeWithoutTokenIs401() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
        verify(userService, never()).getUserProfile(anyLong());
    }

    @Test
    void putMeWithoutTokenIs401() throws Exception {
        mvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"A\",\"lastName\":\"B\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void putMeWithBlankFirstNameIs400WithFieldErrors() throws Exception {
        mvc.perform(put(URL).with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"  \",\"lastName\":\"B\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'firstName')]").exists());

        verify(userService, never()).updateProfile(anyLong(), any());
    }

    @Test
    void putMeWithBadPhoneIs400() throws Exception {
        mvc.perform(put(URL).with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"A\",\"lastName\":\"B\",\"phoneNumber\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'phoneNumber')]").exists());
    }

    @Test
    void putMeIgnoresRoleInBody() throws Exception {
        when(userService.updateProfile(42L, new UpdateProfileRequest("A", "B", null))).thenReturn(profile(42L));

        mvc.perform(put(URL).with(token()).header("X-User-Id", "1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"A\",\"lastName\":\"B\",\"role\":\"ROLE_ADMIN\",\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_USER"));

        // The request type has no role/status, so the service can only ever receive name and phone
        verify(userService).updateProfile(42L, new UpdateProfileRequest("A", "B", null));
    }

    // ---------- change password (NX-309) ----------

    private static final String PASSWORD_URL = URL + "/password";

    @Test
    void changePasswordReturns204AndUsesIdFromToken() throws Exception {
        mvc.perform(put(PASSWORD_URL).with(token()).header("X-User-Id", "1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old-password\",\"newPassword\":\"new-password-1\"}"))
                .andExpect(status().isNoContent());

        verify(userService).changePassword(42L,
                new com.nexusexchange.user.dto.ChangePasswordRequest("old-password", "new-password-1"));
    }

    @Test
    void changePasswordWithShortNewPasswordIs400() throws Exception {
        mvc.perform(put(PASSWORD_URL).with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old-password\",\"newPassword\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'newPassword')]").exists());
        verify(userService, never()).changePassword(anyLong(), any());
    }

    @Test
    void changePasswordWithBlankCurrentPasswordIs400() throws Exception {
        mvc.perform(put(PASSWORD_URL).with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\" \",\"newPassword\":\"new-password-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'currentPassword')]").exists());
    }

    @Test
    void changePasswordWithTooLongNewPasswordIs400() throws Exception {
        mvc.perform(put(PASSWORD_URL).with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old-password\",\"newPassword\":\"" + "a".repeat(73) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePasswordWithWrongCurrentPasswordIs401() throws Exception {
        org.mockito.Mockito.doThrow(new com.nexusexchange.user.exception.InvalidCredentialsException("Current password is incorrect"))
                .when(userService).changePassword(anyLong(), any());

        mvc.perform(put(PASSWORD_URL).with(token()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"bad-password\",\"newPassword\":\"new-password-1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordWithoutTokenIs401() throws Exception {
        mvc.perform(put(PASSWORD_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old-password\",\"newPassword\":\"new-password-1\"}"))
                .andExpect(status().isUnauthorized());
        verify(userService, never()).changePassword(anyLong(), any());
    }
}
