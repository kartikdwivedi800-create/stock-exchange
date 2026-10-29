package com.nexusexchange.user.controller;

import com.nexusexchange.user.dto.AuthResponseDto;
import com.nexusexchange.user.dto.RefreshTokenRequest;
import com.nexusexchange.user.dto.UserLoginDto;
import com.nexusexchange.user.dto.UserRegistrationDto;
import com.nexusexchange.user.dto.UserProfileDto;
import com.nexusexchange.user.service.RefreshTokenService;
import com.nexusexchange.user.service.UserService;
import jakarta.validation.Valid;
import com.nexusexchange.common.constants.ApiConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<UserProfileDto> register(@Valid @RequestBody UserRegistrationDto registrationDto) {
        UserProfileDto profile = userService.registerUser(registrationDto);
        return new ResponseEntity<>(profile, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody UserLoginDto loginDto) {
        AuthResponseDto authResponse = userService.loginUser(loginDto);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(refreshTokenService.refresh(request.refreshToken()));
    }

    /** Always 204: logging out twice is fine and the answer never reveals whether the token existed. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        refreshTokenService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
