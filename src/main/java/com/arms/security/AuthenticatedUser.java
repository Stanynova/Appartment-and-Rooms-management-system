package com.arms.security;

import com.arms.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The authenticated principal carried in the HTTP session.
 *
 * <p>Implements {@link UserDetails} so that Spring Security's standard
 * authentication machinery can be used; the role becomes the authority
 * {@code ROLE_<role>} used by role checks (SRS FR-7.3).</p>
 */
public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final UserRole role;
    private final boolean enabled;
    private final boolean mustChangePassword;

    public AuthenticatedUser(Long id,
                             String username,
                             String passwordHash,
                             String fullName,
                             UserRole role,
                             boolean enabled,
                             boolean mustChangePassword) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.enabled = enabled;
        this.mustChangePassword = mustChangePassword;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }
}
