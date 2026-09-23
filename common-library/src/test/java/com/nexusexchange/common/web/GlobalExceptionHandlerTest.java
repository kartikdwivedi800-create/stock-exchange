package com.nexusexchange.common.web;

import com.nexusexchange.common.exception.ConflictException;
import com.nexusexchange.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void notFoundMapsTo404WithRequestIdEcho() throws Exception {
        mvc.perform(get("/things/missing").header("X-Request-Id", "req-123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Thing missing not found"))
                .andExpect(jsonPath("$.path").value("/things/missing"))
                .andExpect(jsonPath("$.requestId").value("req-123"));
    }

    @Test
    void conflictMapsTo409() throws Exception {
        mvc.perform(get("/things/taken"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void validationErrorsListEachField() throws Exception {
        mvc.perform(post("/things").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("name is required"));
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mvc.perform(post("/things").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void notImplementedIs501() throws Exception {
        mvc.perform(get("/things/later"))
                .andExpect(status().isNotImplemented());
    }

    @Test
    void unexpectedErrorsHideInternalMessage() throws Exception {
        mvc.perform(get("/things/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    record ThingRequest(@NotBlank(message = "name is required") String name) {}

    @RestController
    static class TestController {
        @GetMapping("/things/{id}")
        String get(@PathVariable String id) {
            return switch (id) {
                case "missing" -> throw new ResourceNotFoundException("Thing missing not found");
                case "taken" -> throw new ConflictException("Thing already exists");
                case "later" -> throw new UnsupportedOperationException("Not implemented yet");
                case "boom" -> throw new IllegalStateException("secret internal detail");
                default -> id;
            };
        }

        @PostMapping("/things")
        String create(@Valid @RequestBody ThingRequest request) {
            return request.name();
        }
    }
}
