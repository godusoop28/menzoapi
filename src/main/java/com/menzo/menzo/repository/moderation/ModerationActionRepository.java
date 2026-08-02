package com.menzo.menzo.repository.moderation;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.menzo.menzo.domain.moderation.ModerationAction;
import com.menzo.menzo.domain.moderation.ModerationActionType;

/** Insert-only on purpose — no update/delete methods are ever added here (see ModerationAction). */
public interface ModerationActionRepository extends JpaRepository<ModerationAction, UUID> {

    @Query("""
            SELECT m FROM ModerationAction m
            WHERE (:actorId IS NULL OR m.actor.id = :actorId)
            AND (:actionType IS NULL OR m.actionType = :actionType)
            ORDER BY m.createdAt DESC
            """)
    Page<ModerationAction> search(
            @Param("actorId") UUID actorId,
            @Param("actionType") ModerationActionType actionType,
            Pageable pageable);
}
