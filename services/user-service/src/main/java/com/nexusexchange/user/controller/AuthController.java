package com.nexusexchange.user.controller;

import com.nexusexchange.user.dto.AuthResponseDto;
import com.nexusexchange.user.dto.UserLoginDto;
import com.nexusexchange.user.dto.UserRegistrationDto;
import com.nexusexchange.user.dto.UserProfileDto;
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
}
