package com.arms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.context.MessageSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * FR-4.7 stimulus: a newly created account must change its password at first
 * sign-in. While the flag is set, every function except the account endpoints
 * is refused with an explanatory message.
 */
@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private static final Set<String> EXEMPT_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/me",
            "/api/auth/change-password",
            "/error");

    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

    public PasswordChangeRequiredFilter(ObjectMapper objectMapper, MessageSource messageSource) {
        this.objectMapper = objectMapper;
        this.messageSource = messageSource;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = normalize(request.getRequestURI());
        if (isExempt(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser user
                && user.isMustChangePassword()) {
            String message = messageSource.getMessage(
                    "error.passwordChangeRequired", null, LocaleContextHolder.getLocale());
            JsonResponses.write(response, objectMapper, HttpStatus.FORBIDDEN,
                    "PASSWORD_CHANGE_REQUIRED", message, path);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isExempt(String path) {
        return EXEMPT_PATHS.contains(path)
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.equals("/swagger-ui.html");
    }

    private static String normalize(String uri) {
        if (uri.length() > 1 && uri.endsWith("/")) {
            return uri.substring(0, uri.length() - 1);
        }
        return uri;
    }
}
