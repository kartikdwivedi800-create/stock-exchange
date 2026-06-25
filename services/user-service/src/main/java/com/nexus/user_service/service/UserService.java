package com.nexus.user_service.service;

import com.nexus.user_service.dto.AuthResponseDto;
import com.nexus.user_service.dto.UserLoginDto;
import com.nexus.user_service.dto.UserRegistrationDto;
import com.nexus.user_service.dto.UserProfileDto;

public interface UserService {

    UserProfileDto registerUser(UserRegistrationDto registrationDto);

    AuthResponseDto loginUser(UserLoginDto loginDto);

    UserProfileDto getUserProfile(Long id);

    UserProfileDto updateUserProfile(Long id, UserProfileDto profileDto);
}
