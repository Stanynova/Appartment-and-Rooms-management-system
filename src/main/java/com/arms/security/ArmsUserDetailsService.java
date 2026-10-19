package com.arms.security;

import com.arms.entity.AppUser;
import com.arms.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads accounts for authentication (SRS FR-7.1). Only the salted hash is
 * carried into the principal; it is never logged or returned (FR-7.2).
 */
@Service
public class ArmsUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public ArmsUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return new AuthenticatedUser(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getRole(),
                user.isEnabled(),
                user.isMustChangePassword());
    }
}
