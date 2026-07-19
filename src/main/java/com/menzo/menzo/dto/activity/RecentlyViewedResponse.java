package com.menzo.menzo.dto.activity;

import java.time.Instant;
import java.util.UUID;

public record RecentlyViewedResponse(String kind, UUID id, Instant viewedAt) {
}
