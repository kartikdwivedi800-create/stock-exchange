package com.nexusexchange.common.web;

import com.nexusexchange.common.constants.HeaderConstants;
import com.nexusexchange.common.dto.ErrorResponse;
import com.nexusexchange.common.exception.BadRequestException;
import com.nexusexchange.common.exception.ConflictException;
import com.nexusexchange.common.exception.ForbiddenException;
import com.nexusexchange.common.exception.ResourceNotFoundException;
import com.nexusexchange.common.exception.UnauthorizedException;
import com.nexusexchange.common.exception.ValidationException;
import com.nexusexchange.common.util.RequestIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * One exception-to-HTTP mapping shared by all services (registered by {@link CommonWebAutoConfiguration}).
 * Services throw the exceptions from {@code com.nexusexchange.common.exception} (or subclasses of them).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler({BadRequestException.class, ValidationException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, WebRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex, WebRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        List<ErrorResponse.FieldErrorDetail> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> ErrorResponse.FieldErrorDetail.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .rejectedValue(fe.getRejectedValue())
                        .build())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fields);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception ex, WebRequest request) {
        String message = ex instanceof HttpMessageNotReadableException
                ? "Malformed or unreadable request body"
                : ex.getMessage();
        return build(HttpStatus.BAD_REQUEST, message, request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, "No endpoint " + ex.getHttpMethod() + " /" + ex.getResourcePath(), request, null);
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> handleNotImplemented(UnsupportedOperationException ex, WebRequest request) {
        return build(HttpStatus.NOT_IMPLEMENTED, ex.getMessage(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, WebRequest request) {
        String requestId = requestId(request);
        log.error("Unhandled exception [requestId={}] on {}", requestId, path(request), ex);
        // Never leak internal details to clients
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null, requestId));
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, WebRequest request,
                                                List<ErrorResponse.FieldErrorDetail> fieldErrors) {
        String requestId = requestId(request);
        log.debug("{} {} [requestId={}]: {}", status.value(), path(request), requestId, message);
        return ResponseEntity.status(status).body(body(status, message, request, fieldErrors, requestId));
    }

    private ErrorResponse body(HttpStatus status, String message, WebRequest request,
                               List<ErrorResponse.FieldErrorDetail> fieldErrors, String requestId) {
        return ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path(request))
                .requestId(requestId)
                .fieldErrors(fieldErrors)
                .build();
    }

    private static String requestId(WebRequest request) {
        String header = request.getHeader(HeaderConstants.X_REQUEST_ID);
        return (header != null && !header.isBlank()) ? header : RequestIdGenerator.generateRequestId();
    }

    private static String path(WebRequest request) {
        if (request instanceof NativeWebRequest nwr) {
            HttpServletRequest servlet = nwr.getNativeRequest(HttpServletRequest.class);
            if (servlet != null) {
                return servlet.getRequestURI();
            }
        }
        return request.getDescription(false).replace("uri=", "");
    }
}
