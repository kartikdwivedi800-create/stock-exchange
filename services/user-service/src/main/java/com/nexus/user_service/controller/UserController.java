package com.nexus.user_service.controller;

import com.nexus.user_service.dto.UserProfileDto;
import com.nexus.user_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<UserProfileDto> getProfile(@RequestHeader("X-User-Id") Long userId) {
        UserProfileDto profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<UserProfileDto> updateProfile(@RequestHeader("X-User-Id") Long userId, 
                                                        @RequestBody UserProfileDto profileDto) {
        UserProfileDto updatedProfile = userService.updateUserProfile(userId, profileDto);
        return ResponseEntity.ok(updatedProfile);
    }
}
