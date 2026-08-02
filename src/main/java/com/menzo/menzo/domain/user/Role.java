package com.menzo.menzo.domain.user;

/**
 * Ascending power order — role.ordinal() gives a clean "at least this tier" check
 * in AdminAuthorizationService. Never reorder these without updating that logic.
 */
public enum Role {
    USER,
    CURATOR,
    LEADER,
    MASTER
}
