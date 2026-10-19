package com.arms.controller;

import com.arms.dto.UserDtos.CreateUserRequest;
import com.arms.dto.UserDtos.ResetPasswordRequest;
import com.arms.dto.UserDtos.UpdateUserRequest;
import com.arms.dto.UserDtos.UserResponse;
import com.arms.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * User account administration (SRS FR-7.4). Every function requires the
 * administrator role; accounts are disabled and re-enabled rather than
 * deleted, and every change is audited (FR-7.6).
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.list();
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id) {
        return userService.get(id);
    }

    /** FR-4.7 stimulus: the new account must change its password at first sign-in. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id,
                               @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @PostMapping("/{id}/disable")
    public UserResponse disable(@PathVariable Long id) {
        return userService.setEnabled(id, false);
    }

    @PostMapping("/{id}/enable")
    public UserResponse enable(@PathVariable Long id) {
        return userService.setEnabled(id, true);
    }

    @PostMapping("/{id}/reset-password")
    public UserResponse resetPassword(@PathVariable Long id,
                                      @Valid @RequestBody ResetPasswordRequest request) {
        return userService.resetPassword(id, request);
    }
}
