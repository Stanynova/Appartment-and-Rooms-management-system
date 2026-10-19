package com.arms.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.arms.exception.UnauthorizedException;

/** Resolves the authenticated caller for the service and controller layers. */
@Component
public class CurrentUser {

    public AuthenticatedUser require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new UnauthorizedException("Sign-in is required to use this function.");
    }

    public Long id() {
        return require().getId();
    }

    public String username() {
        return require().getUsername();
    }
}
