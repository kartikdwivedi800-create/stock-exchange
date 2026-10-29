package com.nexusexchange.user.service;

import com.nexusexchange.user.dto.AuthResponseDto;
import com.nexusexchange.user.dto.UpdateProfileRequest;
import com.nexusexchange.user.dto.UserLoginDto;
import com.nexusexchange.user.dto.UserRegistrationDto;
import com.nexusexchange.user.dto.UserProfileDto;

public interface UserService {

    UserProfileDto registerUser(UserRegistrationDto registrationDto);

    AuthResponseDto loginUser(UserLoginDto loginDto);

    UserProfileDto getUserProfile(Long id);

    org.springframework.data.domain.Page<UserProfileDto> listUsers(org.springframework.data.domain.Pageable pageable);

    UserProfileDto updateProfile(Long id, UpdateProfileRequest request);

    void changePassword(Long id, com.nexusexchange.user.dto.ChangePasswordRequest request);
}
