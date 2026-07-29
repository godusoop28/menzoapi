package com.menzo.menzo.dto.voice;

import java.util.List;

import com.menzo.menzo.dto.user.UserSummary;

public record VoiceParticipantsResponse(List<UserSummary> participants) {
}
