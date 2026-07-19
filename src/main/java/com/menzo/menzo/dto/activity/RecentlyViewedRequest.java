package com.menzo.menzo.dto.activity;

import java.util.UUID;

import com.menzo.menzo.domain.activity.ActivityKind;

import jakarta.validation.constraints.NotNull;

public record RecentlyViewedRequest(@NotNull ActivityKind kind, @NotNull UUID id) {
}
