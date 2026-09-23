package com.nexusexchange.user.service;

import com.nexusexchange.user.dto.AuthResponseDto;
import com.nexusexchange.user.dto.UserLoginDto;
import com.nexusexchange.user.dto.UserRegistrationDto;
import com.nexusexchange.user.dto.UserProfileDto;

public interface UserService {

    UserProfileDto registerUser(UserRegistrationDto registrationDto);

    AuthResponseDto loginUser(UserLoginDto loginDto);

    UserProfileDto getUserProfile(Long id);

    UserProfileDto updateUserProfile(Long id, UserProfileDto profileDto);
}
