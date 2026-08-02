package com.menzo.menzo.controller;

import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.common.PageResponse;
import com.menzo.menzo.dto.moderation.ReasonRequest;
import com.menzo.menzo.dto.sticker.CreateStickerPackRequest;
import com.menzo.menzo.dto.sticker.StickerPackDetailResponse;
import com.menzo.menzo.dto.sticker.StickerPackSummaryResponse;
import com.menzo.menzo.service.StickerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/stickers")
public class StickerController {

    private final StickerService stickerService;

    public StickerController(StickerService stickerService) {
        this.stickerService = stickerService;
    }

    @PostMapping("/packs")
    @ResponseStatus(HttpStatus.CREATED)
    public StickerPackDetailResponse createPack(
            @AuthenticationPrincipal User me, @Valid @RequestBody CreateStickerPackRequest request) {
        return stickerService.createPack(me, request);
    }

    @GetMapping("/packs")
    public PageResponse<StickerPackSummaryResponse> listPacks(
            @RequestParam(required = false) String query, @PageableDefault(size = 24) Pageable pageable) {
        return stickerService.listPacks(query, pageable);
    }

    @GetMapping("/packs/{id}")
    public StickerPackDetailResponse getPack(@PathVariable UUID id) {
        return stickerService.getPack(id);
    }

    @DeleteMapping("/packs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePack(
            @PathVariable UUID id,
            @AuthenticationPrincipal User me,
            @RequestBody(required = false) ReasonRequest request) {
        stickerService.deletePack(me, id, request != null ? request.reason() : null);
    }
}
