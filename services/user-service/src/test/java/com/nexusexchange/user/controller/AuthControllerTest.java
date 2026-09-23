package com.nexusexchange.user.controller;

import com.nexusexchange.user.exception.InvalidCredentialsException;
import com.nexusexchange.user.exception.UserAlreadyExistsException;
import com.nexusexchange.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Checks the /api/v1 routes and that the shared GlobalExceptionHandler from common-library
 * is picked up automatically (no handler exists in user-service itself).
 */
@WebMvcTest(controllers = {AuthController.class, UserController.class})
class AuthControllerTest {

    private static final String VALID_REGISTRATION = """
            {"username":"kartik","email":"kartik@example.com","password":"Secret123!",
             "firstName":"Kartik","lastName":"D","phoneNumber":"9999999999"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserService userService;

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
    void profileWithoutUserHeaderReturns400() throws Exception {
        mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oldUnprefixedRouteIsGone() throws Exception {
        mvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content(VALID_REGISTRATION))
                .andExpect(status().isNotFound());
    }
}
