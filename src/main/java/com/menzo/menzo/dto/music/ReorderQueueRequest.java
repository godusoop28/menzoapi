package com.menzo.menzo.dto.music;

import java.util.List;
import java.util.UUID;

public record ReorderQueueRequest(List<UUID> orderedQueueItemIds, Long expectedVersion) {
}
