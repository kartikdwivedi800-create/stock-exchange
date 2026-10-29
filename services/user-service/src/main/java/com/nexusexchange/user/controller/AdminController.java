package com.nexusexchange.user.controller;

import com.nexusexchange.common.constants.ApiConstants;
import com.nexusexchange.user.dto.UserProfileDto;
import com.nexusexchange.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.API_V1 + "/admin")
@RequiredArgsConstructor
public class AdminController {

    static final int MAX_PAGE_SIZE = 100;

    private final UserService userService;

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public PagedModel<UserProfileDto> listUsers(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return new PagedModel<>(userService.listUsers(PageRequest.of(safePage, safeSize, Sort.by("id"))));
    }
}
