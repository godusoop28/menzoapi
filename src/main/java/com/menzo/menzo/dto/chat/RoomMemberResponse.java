package com.menzo.menzo.dto.chat;

import java.time.Instant;

import com.menzo.menzo.dto.user.UserSummary;

public record RoomMemberResponse(UserSummary user, String role, Instant joinedAt) {
}
