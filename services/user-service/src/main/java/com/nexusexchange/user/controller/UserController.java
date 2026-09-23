package com.nexusexchange.user.controller;

import com.nexusexchange.user.dto.UserProfileDto;
import com.nexusexchange.user.service.UserService;
import com.nexusexchange.common.constants.ApiConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/users/me")
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
