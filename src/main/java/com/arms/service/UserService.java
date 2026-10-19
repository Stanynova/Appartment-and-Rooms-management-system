package com.arms.service;

import com.arms.dto.UserDtos.CreateUserRequest;
import com.arms.dto.UserDtos.ResetPasswordRequest;
import com.arms.dto.UserDtos.UpdateUserRequest;
import com.arms.dto.UserDtos.UserResponse;
import com.arms.entity.AppUser;
import com.arms.entity.AuditEventType;
import com.arms.exception.BadRequestException;
import com.arms.exception.ConflictException;
import com.arms.exception.NotFoundException;
import com.arms.mapper.DtoMapper;
import com.arms.repository.UserRepository;
import com.arms.security.CurrentUser;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * User account administration (SRS FR-7.4, BR-7).
 *
 * <p>Accounts are disabled rather than deleted so that historical audit
 * entries keep their meaning. Passwords are stored only as salted hashes
 * (FR-7.2).</p>
 */
@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;
    private final AuditService audit;
    private final Messages messages;

    public UserService(UserRepository users,
                       PasswordEncoder passwordEncoder,
                       CurrentUser currentUser,
                       AuditService audit,
                       Messages messages) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
        this.audit = audit;
        this.messages = messages;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return users.findAll(Sort.by("username")).stream()
                .map(DtoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return DtoMapper.toResponse(require(id));
    }

    /** FR-4.7 stimulus: a new account must change its password at first sign-in. */
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        // Trim before the uniqueness check so the stored value and the
        // value checked against uq_users_username are always identical.
        String username = request.username().trim();
        if (users.existsByUsername(username)) {
            throw new ConflictException(messages.get("user.usernameExists"), "USERNAME_EXISTS");
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName() == null || request.fullName().isBlank()
                ? null
                : request.fullName().trim());
        user.setRole(request.role());
        user.setEnabled(true);
        user.setMustChangePassword(true);
        users.save(user);

        audit.record(AuditEventType.USER_CREATED, currentUser.username(), currentUser.id(),
                "Created account '" + user.getUsername() + "' with role " + user.getRole());
        return DtoMapper.toResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        AppUser user = require(id);
        user.setFullName(request.fullName() == null || request.fullName().isBlank()
                ? user.getFullName()
                : request.fullName().trim());
        user.setRole(request.role());
        users.save(user);

        audit.record(AuditEventType.USER_UPDATED, currentUser.username(), currentUser.id(),
                "Updated account '" + user.getUsername() + "', role " + user.getRole());
        return DtoMapper.toResponse(user);
    }

    @Transactional
    public UserResponse setEnabled(Long id, boolean enabled) {
        AppUser user = require(id);
        if (!enabled && user.getId().equals(currentUser.id())) {
            throw new ConflictException(messages.get("user.selfDisable"), "SELF_DISABLE");
        }
        user.setEnabled(enabled);
        users.save(user);

        audit.record(enabled ? AuditEventType.USER_ENABLED : AuditEventType.USER_DISABLED,
                currentUser.username(), currentUser.id(),
                (enabled ? "Enabled" : "Disabled") + " account '" + user.getUsername() + "'");
        return DtoMapper.toResponse(user);
    }

    @Transactional
    public UserResponse resetPassword(Long id, ResetPasswordRequest request) {
        AppUser user = require(id);
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(true);
        users.save(user);

        audit.record(AuditEventType.PASSWORD_RESET, currentUser.username(), currentUser.id(),
                "Password reset for account '" + user.getUsername() + "'");
        return DtoMapper.toResponse(user);
    }

    /**
     * FR-4.7 / FR-7.1 first sign-in: the account holder replaces their own
     * password. The session is ended by the controller so that the next
     * request authenticates against the new credentials.
     */
    @Transactional
    public void changePassword(com.arms.dto.AuthDtos.ChangePasswordRequest request) {
        AppUser user = require(currentUser.id());
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException(
                    messages.get("auth.passwordMismatch"), "PASSWORD_MISMATCH");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException(messages.get("auth.passwordSame"), "PASSWORD_SAME");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        users.save(user);

        audit.record(AuditEventType.PASSWORD_CHANGED, user.getUsername(), user.getId(),
                "Password changed by the account holder");
    }

    private AppUser require(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        messages.get("user.notFound"), "USER_NOT_FOUND"));
    }
}
