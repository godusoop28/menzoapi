package com.menzo.menzo.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.moderation.ModerationAction;
import com.menzo.menzo.domain.moderation.ModerationActionType;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.common.PageResponse;
import com.menzo.menzo.dto.moderation.ModerationActionResponse;
import com.menzo.menzo.repository.moderation.ModerationActionRepository;
import com.menzo.menzo.service.mapper.ProfileMapper;

/**
 * Insert-only audit log for every LEADER/CURATOR/MASTER-authorized moderation action. Routine
 * room-owner self-moderation (existing kick/promote/demote by a room's own OWNER/CO_HOST) is
 * intentionally NOT logged here — it was never asked to be tracked and would flood the log with
 * unrelated noise. record() is meant to be called as the last step inside the same transactional
 * service method as the action itself; Spring's default REQUIRED propagation joins the caller's
 * transaction automatically.
 */
@Service
public class ModerationLogService {

    private final ModerationActionRepository moderationActionRepository;
    private final ProfileMapper profileMapper;

    public ModerationLogService(ModerationActionRepository moderationActionRepository, ProfileMapper profileMapper) {
        this.moderationActionRepository = moderationActionRepository;
        this.profileMapper = profileMapper;
    }

    @Transactional
    public void record(User actor, ModerationActionType type, String targetType, UUID targetId, String reason) {
        moderationActionRepository.save(new ModerationAction(actor, type, targetType, targetId, reason));
    }

    @Transactional(readOnly = true)
    public PageResponse<ModerationActionResponse> search(UUID actorId, ModerationActionType actionType, Pageable pageable) {
        Page<ModerationAction> page = moderationActionRepository.search(actorId, actionType, pageable);
        return PageResponse.of(page, this::toResponse);
    }

    private ModerationActionResponse toResponse(ModerationAction action) {
        return new ModerationActionResponse(
                action.getId(),
                profileMapper.toSummary(action.getActor()),
                action.getActionType().name(),
                action.getTargetType(),
                action.getTargetId(),
                action.getReason(),
                action.getCreatedAt());
    }
}
