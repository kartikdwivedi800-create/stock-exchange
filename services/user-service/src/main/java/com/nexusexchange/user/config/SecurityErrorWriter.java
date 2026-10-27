package com.nexusexchange.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusexchange.common.constants.HeaderConstants;
import com.nexusexchange.common.dto.ErrorResponse;
import com.nexusexchange.common.util.RequestIdGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Writes the common {@link ErrorResponse} as JSON from security filters, where
 * the @RestControllerAdvice is never reached.
 */
final class SecurityErrorWriter {

    private SecurityErrorWriter() {
    }

    static void write(ObjectMapper objectMapper, HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, String message) throws IOException {
        String header = request.getHeader(HeaderConstants.X_REQUEST_ID);
        String requestId = (header != null && !header.isBlank()) ? header : RequestIdGenerator.generateRequestId();

        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .requestId(requestId)
                .build();

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
