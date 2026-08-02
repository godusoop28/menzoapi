package com.menzo.menzo.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.moderation.ModerationActionType;
import com.menzo.menzo.domain.sticker.Sticker;
import com.menzo.menzo.domain.sticker.StickerPack;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.common.PageResponse;
import com.menzo.menzo.dto.sticker.CreateStickerPackRequest;
import com.menzo.menzo.dto.sticker.StickerPackDetailResponse;
import com.menzo.menzo.dto.sticker.StickerPackSummaryResponse;
import com.menzo.menzo.dto.sticker.StickerResponse;
import com.menzo.menzo.exception.ForbiddenException;
import com.menzo.menzo.exception.NotFoundException;
import com.menzo.menzo.repository.sticker.StickerPackRepository;
import com.menzo.menzo.repository.sticker.StickerRepository;
import com.menzo.menzo.service.mapper.ProfileMapper;

/**
 * Público desde el instante en que se crea (ver StickerPack) — no hay endpoint de "agregar a mi
 * bandeja". Cualquier usuario logueado puede crear un pack y cualquier otro puede usarlo de
 * inmediato.
 */
@Service
public class StickerService {

    private final StickerPackRepository stickerPackRepository;
    private final StickerRepository stickerRepository;
    private final ProfileMapper profileMapper;
    private final AdminAuthorizationService adminAuthorizationService;
    private final ModerationLogService moderationLogService;

    public StickerService(
            StickerPackRepository stickerPackRepository,
            StickerRepository stickerRepository,
            ProfileMapper profileMapper,
            AdminAuthorizationService adminAuthorizationService,
            ModerationLogService moderationLogService) {
        this.stickerPackRepository = stickerPackRepository;
        this.stickerRepository = stickerRepository;
        this.profileMapper = profileMapper;
        this.adminAuthorizationService = adminAuthorizationService;
        this.moderationLogService = moderationLogService;
    }

    @Transactional
    public StickerPackDetailResponse createPack(User me, CreateStickerPackRequest request) {
        StickerPack pack = new StickerPack();
        pack.setCreator(me);
        pack.setName(request.name().trim());
        pack = stickerPackRepository.saveAndFlush(pack);

        List<Sticker> stickers = new java.util.ArrayList<>();
        int order = 0;
        for (String url : request.imageUrls()) {
            Sticker sticker = new Sticker();
            sticker.setPack(pack);
            sticker.setImageUrl(url);
            sticker.setSortOrder(order++);
            stickers.add(sticker);
        }
        stickers = stickerRepository.saveAll(stickers);

        return toDetailResponse(pack, stickers);
    }

    @Transactional(readOnly = true)
    public PageResponse<StickerPackSummaryResponse> listPacks(String query, Pageable pageable) {
        Page<StickerPack> page = stickerPackRepository.search(
                query == null || query.isBlank() ? null : query.trim(), pageable);
        return PageResponse.of(page, this::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public StickerPackDetailResponse getPack(UUID packId) {
        StickerPack pack = requirePack(packId);
        List<Sticker> stickers = stickerRepository.findByPackIdOrderBySortOrderAsc(packId);
        return toDetailResponse(pack, stickers);
    }

    /** El creador siempre puede borrar el suyo, sin motivo. Un no-creador necesita LEADER+ y un
     * motivo obligatorio, igual que el borrado de publicaciones — consistente con el resto de la
     * moderación de contenido. */
    @Transactional
    public void deletePack(User me, UUID packId, String staffReason) {
        StickerPack pack = requirePack(packId);
        boolean isCreator = pack.getCreator().getId().equals(me.getId());
        if (!isCreator) {
            adminAuthorizationService.requireLeader(me);
            if (staffReason == null || staffReason.isBlank()) {
                throw new ForbiddenException("Necesitás indicar un motivo");
            }
        }
        UUID packIdCopy = pack.getId();
        stickerRepository.deleteByPackId(packId);
        stickerPackRepository.delete(pack);
        if (!isCreator) {
            moderationLogService.record(me, ModerationActionType.DELETE_STICKER_PACK, "STICKER_PACK", packIdCopy, staffReason);
        }
    }

    private StickerPack requirePack(UUID packId) {
        return stickerPackRepository.findById(packId)
                .orElseThrow(() -> new NotFoundException("Sticker pack no encontrado"));
    }

    private StickerPackSummaryResponse toSummaryResponse(StickerPack pack) {
        List<Sticker> stickers = stickerRepository.findByPackIdOrderBySortOrderAsc(pack.getId());
        String cover = stickers.isEmpty() ? null : stickers.get(0).getImageUrl();
        return new StickerPackSummaryResponse(
                pack.getId(), pack.getName(), profileMapper.toSummary(pack.getCreator()), cover, stickers.size(), pack.getCreatedAt());
    }

    private StickerPackDetailResponse toDetailResponse(StickerPack pack, List<Sticker> stickers) {
        List<StickerResponse> stickerResponses = stickers.stream()
                .map(s -> new StickerResponse(s.getId(), s.getImageUrl(), s.getSortOrder()))
                .toList();
        return new StickerPackDetailResponse(
                pack.getId(), pack.getName(), profileMapper.toSummary(pack.getCreator()), stickerResponses, pack.getCreatedAt());
    }
}
