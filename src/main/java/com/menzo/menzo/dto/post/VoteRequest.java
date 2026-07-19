package com.menzo.menzo.dto.post;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record VoteRequest(@NotNull UUID optionId) {
}
