package com.nexus.user_service.controller;

import com.nexus.user_service.dto.AuthResponseDto;
import com.nexus.user_service.dto.UserLoginDto;
import com.nexus.user_service.dto.UserRegistrationDto;
import com.nexus.user_service.dto.UserProfileDto;
import com.nexus.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
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
