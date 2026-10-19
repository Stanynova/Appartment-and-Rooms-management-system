package com.arms.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security setup for the REST API.
 *
 * <p>SRS 3.4 requires session state to be carried in an HttpOnly cookie, so
 * authentication is session-based: {@code POST /api/auth/login} opens a
 * session and every subsequent request is authenticated from that session.
 * Passwords are stored only as salted BCrypt hashes (FR-7.2) and permissions
 * are granted to roles (FR-7.3), enforced on controllers with
 * {@code @PreAuthorize}.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** SRS FR-7.2: salted hashes only. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   ObjectMapper objectMapper,
                                                   MessageSource messageSource) throws Exception {
        http
                // Session-cookie authentication for a JSON API: CSRF tokens are not
                // carried by API clients; the SameSite=Strict session cookie mitigates
                // cross-site request forgery for browser clients.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/logout").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint(objectMapper, messageSource))
                        .accessDeniedHandler(accessDeniedHandler(objectMapper, messageSource)));
        return http.build();
    }

    private AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper,
                                                              MessageSource messageSource) {
        return (request, response, authException) ->
                JsonResponses.write(response, objectMapper, HttpStatus.UNAUTHORIZED,
                        "UNAUTHORIZED",
                        messageSource.getMessage("error.unauthorized", null, request.getLocale()),
                        request.getRequestURI());
    }

    private AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper,
                                                    MessageSource messageSource) {
        return (request, response, accessDeniedException) ->
                JsonResponses.write(response, objectMapper, HttpStatus.FORBIDDEN,
                        "FORBIDDEN",
                        messageSource.getMessage("error.forbidden", null, request.getLocale()),
                        request.getRequestURI());
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${arms.cors.allowed-origins}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(
                Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Accept", "X-Requested-With"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
