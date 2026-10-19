package com.arms.config;

import com.arms.entity.AppUser;
import com.arms.entity.AuditEventType;
import com.arms.entity.UserRole;
import com.arms.repository.UserRepository;
import com.arms.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the initial administrator account on first start so that the system
 * can be used immediately (SRS assumption A-01: initial setup data is
 * captured by the administrator).
 *
 * <p>The seeded account must change its password at first sign-in, which
 * demonstrates the FR-4.7 / FR-7.1 first sign-in requirement. Credentials can
 * be overridden with the {@code ARMS_DEFAULT_ADMIN_USERNAME} and
 * {@code ARMS_DEFAULT_ADMIN_PASSWORD} environment variables.</p>
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    @Value("${arms.security.default-admin-username}")
    private String adminUsername;

    @Value("${arms.security.default-admin-password}")
    private String adminPassword;

    public DataInitializer(UserRepository users,
                           PasswordEncoder passwordEncoder,
                           AuditService audit) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }

        AppUser admin = new AppUser();
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setFullName("System Administrator");
        admin.setRole(UserRole.ADMIN);
        admin.setEnabled(true);
        admin.setMustChangePassword(true);
        users.save(admin);

        audit.record(AuditEventType.USER_CREATED, adminUsername, admin.getId(),
                "Initial administrator account created on first start");

        log.info("Initial administrator account '{}' created. Sign in and change the password "
                + "when prompted (default credentials come from application.yml and can be "
                + "overridden with environment variables).", adminUsername);
    }
}
