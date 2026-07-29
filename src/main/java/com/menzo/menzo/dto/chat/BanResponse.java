package com.menzo.menzo.dto.chat;

import java.time.Instant;

import com.menzo.menzo.dto.user.UserSummary;

public record BanResponse(UserSummary user, String reason, Instant createdAt, UserSummary bannedBy) {
}
