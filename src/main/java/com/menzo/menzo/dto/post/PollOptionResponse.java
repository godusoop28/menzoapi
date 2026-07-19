package com.menzo.menzo.dto.post;

import java.util.UUID;

public record PollOptionResponse(UUID id, String label, long voteCount, boolean votedByMe) {
}
