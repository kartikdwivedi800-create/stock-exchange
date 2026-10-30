package com.nexusexchange.user.controller;

import com.nexusexchange.user.dto.ChangePasswordRequest;
import com.nexusexchange.user.dto.UpdateProfileRequest;
import com.nexusexchange.user.dto.UserProfileDto;
import com.nexusexchange.user.service.UserService;
import com.nexusexchange.common.constants.ApiConstants;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserProfileDto> getProfile(@AuthenticationPrincipal Jwt jwt) {
        UserProfileDto profile = userService.getUserProfile(userId(jwt));
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<UserProfileDto> updateProfile(@AuthenticationPrincipal Jwt jwt,
                                                        @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileDto updatedProfile = userService.updateProfile(userId(jwt), request);
        return ResponseEntity.ok(updatedProfile);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId(jwt), request);
        return ResponseEntity.noContent().build();
    }

    /** The id comes from the verified token, never from a client-controlled header. */
    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
