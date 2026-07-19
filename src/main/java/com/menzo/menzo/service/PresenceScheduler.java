package com.menzo.menzo.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.repository.user.UserRepository;

@Component
public class PresenceScheduler {

    private static final int STALE_AFTER_MINUTES = 5;

    private final UserRepository userRepository;

    public PresenceScheduler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void markStaleUsersOffline() {
        Instant threshold = Instant.now().minus(STALE_AFTER_MINUTES, ChronoUnit.MINUTES);
        userRepository.markStaleUsersOffline(threshold);
    }
}
