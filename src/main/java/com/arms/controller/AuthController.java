package com.arms.controller;

import com.arms.dto.AuthDtos.ChangePasswordRequest;
import com.arms.dto.AuthDtos.LoginRequest;
import com.arms.dto.AuthDtos.SessionResponse;
import com.arms.entity.AuditEventType;
import com.arms.exception.UnauthorizedException;
import com.arms.security.AuthenticatedUser;
import com.arms.security.CurrentUser;
import com.arms.service.AuditService;
import com.arms.service.Messages;
import com.arms.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints (SRS FR-7.1, FR-7.2, FR-7.5 and SRS 3.4).
 *
 * <p>{@code POST /login} authenticates the credentials and stores the
 * security context in the HTTP session, which is carried in an HttpOnly
 * cookie. Failed sign-ins are audited (FR-7.6).</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final AuditService audit;
    private final CurrentUser currentUser;
    private final Messages messages;

    public AuthController(AuthenticationManager authenticationManager,
                          UserService userService,
                          AuditService audit,
                          CurrentUser currentUser,
                          Messages messages) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.audit = audit;
        this.currentUser = currentUser;
        this.messages = messages;
    }

    @PostMapping("/login")
    public ResponseEntity<SessionResponse> login(@Valid @RequestBody LoginRequest request,
                                                 HttpServletRequest httpRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));

            // Session fixation protection (SRS FR-7.5): a session that existed
            // before sign-in is given a fresh id, so an identifier chosen by an
            // attacker cannot become the authenticated session.
            if (httpRequest.getSession(false) != null) {
                httpRequest.changeSessionId();
            }

            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            // SRS 3.4: session state is carried in a cookie marked HttpOnly
            httpRequest.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
            audit.record(AuditEventType.SIGN_IN, user.getUsername(), user.getId(),
                    "Successful sign-in");
            return ResponseEntity.ok(toSession(user));
        } catch (AuthenticationException ex) {
            // FR-7.6: failed sign-ins are recorded; invalid credentials are never echoed
            audit.record(AuditEventType.FAILED_SIGN_IN, request.username(), null,
                    "Failed sign-in attempt");
            throw new UnauthorizedException(messages.get("error.unauthorized"), "UNAUTHORIZED");
        }
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    @GetMapping("/me")
    public SessionResponse me() {
        return toSession(currentUser.require());
    }

    /** FR-7.5 first sign-in: change the password, then re-authenticate. */
    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request,
                               HttpServletRequest httpRequest) {
        userService.changePassword(request);
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
    }

    private static SessionResponse toSession(AuthenticatedUser user) {
        return new SessionResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole().name(),
                user.isMustChangePassword());
    }
}
