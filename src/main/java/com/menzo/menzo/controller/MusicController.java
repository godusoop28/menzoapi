package com.menzo.menzo.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.music.AddQueueItemRequest;
import com.menzo.menzo.dto.music.MusicSessionResponse;
import com.menzo.menzo.dto.music.MusicSettingsRequest;
import com.menzo.menzo.dto.music.QueueItemResponse;
import com.menzo.menzo.dto.music.ReorderQueueRequest;
import com.menzo.menzo.dto.music.RequestSongRequest;
import com.menzo.menzo.dto.music.SeekRequest;
import com.menzo.menzo.dto.music.VersionedRequest;
import com.menzo.menzo.dto.music.YoutubeSearchResult;
import com.menzo.menzo.service.MusicService;

import jakarta.validation.Valid;

/** Menzi DJ — no es un participante del LIVE, es este módulo. Ver MusicService para la
 * arquitectura completa (estado canónico, cola, solicitudes). */
@RestController
@RequestMapping("/api/chat/rooms/{roomId}/live/music")
public class MusicController {

    private final MusicService musicService;

    public MusicController(MusicService musicService) {
        this.musicService = musicService;
    }

    @GetMapping
    public MusicSessionResponse snapshot(@PathVariable UUID roomId, @AuthenticationPrincipal User viewer) {
        return musicService.getSnapshot(roomId, viewer);
    }

    @GetMapping("/search")
    public List<YoutubeSearchResult> search(
            @PathVariable UUID roomId, @RequestParam String q, @AuthenticationPrincipal User actor) {
        return musicService.search(roomId, actor, q);
    }

    @GetMapping("/queue")
    public List<QueueItemResponse> queue(@PathVariable UUID roomId, @AuthenticationPrincipal User viewer) {
        return musicService.listQueue(roomId, viewer);
    }

    @PostMapping("/queue")
    @ResponseStatus(HttpStatus.CREATED)
    public MusicSessionResponse addToQueue(
            @PathVariable UUID roomId, @AuthenticationPrincipal User actor, @Valid @RequestBody AddQueueItemRequest request) {
        return musicService.addToQueue(roomId, actor, request);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestSong(
            @PathVariable UUID roomId, @AuthenticationPrincipal User actor, @Valid @RequestBody RequestSongRequest request) {
        musicService.requestSong(roomId, actor, request);
    }

    @PostMapping("/requests/{requestId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@PathVariable UUID roomId, @PathVariable UUID requestId, @AuthenticationPrincipal User actor) {
        musicService.approveRequest(roomId, actor, requestId);
    }

    @PostMapping("/requests/{requestId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable UUID roomId, @PathVariable UUID requestId, @AuthenticationPrincipal User actor) {
        musicService.rejectRequest(roomId, actor, requestId);
    }

    @PostMapping("/play")
    public MusicSessionResponse play(
            @PathVariable UUID roomId, @AuthenticationPrincipal User actor, @RequestBody(required = false) VersionedRequest request) {
        return musicService.play(roomId, actor, request);
    }

    @PostMapping("/pause")
    public MusicSessionResponse pause(
            @PathVariable UUID roomId, @AuthenticationPrincipal User actor, @RequestBody(required = false) VersionedRequest request) {
        return musicService.pause(roomId, actor, request);
    }

    @PostMapping("/resume")
    public MusicSessionResponse resume(
            @PathVariable UUID roomId, @AuthenticationPrincipal User actor, @RequestBody(required = false) VersionedRequest request) {
        return musicService.resume(roomId, actor, request);
    }

    @PostMapping("/seek")
    public MusicSessionResponse seek(
            @PathVariable UUID roomId, @AuthenticationPrincipal User actor, @Valid @RequestBody SeekRequest request) {
        return musicService.seek(roomId, actor, request);
    }

    @PostMapping("/skip")
    public MusicSessionResponse skip(
            @PathVariable UUID roomId, @AuthenticationPrincipal User actor, @RequestBody(required = false) VersionedRequest request) {
        return musicService.skip(roomId, actor, request);
    }

    @PostMapping("/stop")
    public MusicSessionResponse stop(
            @PathVariable UUID roomId, @AuthenticationPrincipal User actor, @RequestBody(required = false) VersionedRequest request) {
        return musicService.stop(roomId, actor, request);
    }

    @PatchMapping("/settings")
    public MusicSessionResponse updateSettings(
            @PathVariable UUID roomId, @AuthenticationPrincipal User actor, @RequestBody MusicSettingsRequest request) {
        return musicService.updateSettings(roomId, actor, request);
    }

    @PatchMapping("/queue/reorder")
    public MusicSessionResponse reorderQueue(
            @PathVariable UUID roomId, @AuthenticationPrincipal User actor, @RequestBody ReorderQueueRequest request) {
        return musicService.reorderQueue(roomId, actor, request);
    }

    @DeleteMapping("/queue/{queueItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeQueueItem(@PathVariable UUID roomId, @PathVariable UUID queueItemId, @AuthenticationPrincipal User actor) {
        musicService.removeQueueItem(roomId, actor, queueItemId);
    }

    @DeleteMapping("/queue")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearQueue(@PathVariable UUID roomId, @AuthenticationPrincipal User actor) {
        musicService.clearQueue(roomId, actor);
    }
}
