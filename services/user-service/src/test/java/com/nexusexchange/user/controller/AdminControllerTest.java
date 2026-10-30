package com.nexusexchange.user.controller;

import com.nexusexchange.user.config.JsonAccessDeniedHandler;
import com.nexusexchange.user.config.JsonAuthenticationEntryPoint;
import com.nexusexchange.user.config.SecurityConfig;
import com.nexusexchange.user.dto.UserProfileDto;
import com.nexusexchange.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** NX-306: only ROLE_ADMIN may list users. */
@WebMvcTest(controllers = AdminController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class})
class AdminControllerTest {

    private static final String URL = "/api/v1/admin/users";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor tokenWithRole(String role) {
        return jwt().jwt(j -> j.subject("1").claim("role", role))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(role));
    }

    @Test
    void adminGets200WithPage() throws Exception {
        Page<UserProfileDto> page = new PageImpl<>(
                List.of(UserProfileDto.builder().id(1L).username("admin").build()));
        when(userService.listUsers(any(Pageable.class))).thenReturn(page);

        mvc.perform(get(URL).param("page", "0").param("size", "10").with(tokenWithRole("ROLE_ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("admin"));
    }

    @Test
    void sizeIsCappedAt100() throws Exception {
        when(userService.listUsers(any(Pageable.class))).thenReturn(Page.empty());

        mvc.perform(get(URL).param("size", "100000").with(tokenWithRole("ROLE_ADMIN")))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userService).listUsers(pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void userRoleGets403Json() throws Exception {
        mvc.perform(get(URL).with(tokenWithRole("ROLE_USER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403));
        verify(userService, never()).listUsers(any());
    }

    @Test
    void noTokenGets401() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }
}
