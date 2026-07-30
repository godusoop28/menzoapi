package com.menzo.menzo.repository.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.menzo.menzo.domain.chat.LiveMusicQueueItem;
import com.menzo.menzo.domain.chat.QueueItemStatus;

public interface LiveMusicQueueItemRepository extends JpaRepository<LiveMusicQueueItem, UUID> {

    List<LiveMusicQueueItem> findByMusicSessionIdAndStatusOrderByPositionAsc(UUID musicSessionId, QueueItemStatus status);

    List<LiveMusicQueueItem> findByMusicSessionIdAndStatusOrderByCreatedAtAsc(UUID musicSessionId, QueueItemStatus status);

    Optional<LiveMusicQueueItem> findFirstByMusicSessionIdAndStatusOrderByPositionAsc(UUID musicSessionId, QueueItemStatus status);

    Optional<LiveMusicQueueItem> findByIdAndMusicSessionId(UUID id, UUID musicSessionId);

    long countByMusicSessionIdAndStatus(UUID musicSessionId, QueueItemStatus status);

    List<LiveMusicQueueItem> findTop20ByMusicSessionIdAndStatusInOrderByCreatedAtDesc(
            UUID musicSessionId, List<QueueItemStatus> statuses);
}
