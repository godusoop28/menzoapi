package com.menzo.menzo.service;

import org.springframework.stereotype.Service;

import com.menzo.menzo.domain.user.Role;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.exception.ForbiddenException;

/**
 * Guard calls for the global CURATOR/LEADER/MASTER staff tiers — same imperative require* pattern
 * already used everywhere else in this codebase (e.g. requireOwnerOrCoHost), not Spring Method
 * Security. Always called with a freshly-loaded-from-DB User (JwtAuthenticationFilter re-fetches
 * per request), so a demotion takes effect on the very next call, not at next token refresh.
 */
@Service
public class AdminAuthorizationService {

    public void requireCurator(User me) {
        if (me.getRole().ordinal() < Role.CURATOR.ordinal()) {
            throw new ForbiddenException("Necesitás ser curador o superior para hacer esto");
        }
    }

    public void requireLeader(User me) {
        if (me.getRole().ordinal() < Role.LEADER.ordinal()) {
            throw new ForbiddenException("Necesitás ser líder o superior para hacer esto");
        }
    }

    public void requireMaster(User me) {
        if (me.getRole() != Role.MASTER) {
            throw new ForbiddenException("Solo el usuario maestro puede hacer esto");
        }
    }
}
