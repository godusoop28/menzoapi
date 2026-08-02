package com.menzo.menzo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.user.Role;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.repository.user.UserRepository;

/**
 * Re-checks the configured MASTER email on every boot, not just at the V21 migration. Makes
 * menzo.admin.master-email genuinely reconfigurable later without a new migration, and is a
 * second safety net if the account didn't exist yet when V21 ran.
 */
@Component
public class MasterAccountBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AdminProperties adminProperties;

    public MasterAccountBootstrap(UserRepository userRepository, AdminProperties adminProperties) {
        this.userRepository = userRepository;
        this.adminProperties = adminProperties;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String masterEmail = adminProperties.getMasterEmail().trim();
        if (masterEmail.isEmpty()) {
            return;
        }
        userRepository.findByEmailIgnoreCase(masterEmail).ifPresent(user -> {
            if (user.getRole() != Role.MASTER) {
                user.setRole(Role.MASTER);
                userRepository.save(user);
            }
        });
    }
}
