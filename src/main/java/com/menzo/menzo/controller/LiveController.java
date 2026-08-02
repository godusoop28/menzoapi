package com.menzo.menzo.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.live.LiveParticipantResponse;
import com.menzo.menzo.dto.live.LiveSessionResponse;
import com.menzo.menzo.dto.live.LiveTokenResponse;
import com.menzo.menzo.dto.live.MicrophoneRequest;
import com.menzo.menzo.dto.live.ScreenShareRequest;
import com.menzo.menzo.dto.live.StartLiveRequest;
import com.menzo.menzo.dto.live.UpdateLiveRequest;
import com.menzo.menzo.dto.moderation.ReasonRequest;
import com.menzo.menzo.service.LiveService;

import jakarta.validation.Valid;

/**
 * Endpoints nuevos y aditivos para el LIVE moderado (roles, solicitudes para hablar, tokens
 * Agora por rol). No reemplaza /api/chat/rooms/{id}/voice/* (VoiceController) — ese sigue
 * intacto para la app móvil actual. Ver LiveService para el porqué.
 */
@RestController
@RequestMapping("/api/chat/rooms/{id}/live")
public class LiveController {

    private final LiveService liveService;

    public LiveController(LiveService liveService) {
        this.liveService = liveService;
    }

    @GetMapping
    public ResponseEntity<LiveSessionResponse> state(@PathVariable UUID id, @AuthenticationPrincipal User viewer) {
        LiveSessionResponse state = liveService.getState(id, viewer);
        return state == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(state);
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.CREATED)
    public LiveSessionResponse start(
            @PathVariable UUID id, @AuthenticationPrincipal User me, @RequestBody(required = false) StartLiveRequest request) {
        return liveService.startLive(me, id, request);
    }

    @PostMapping("/end")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void end(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        liveService.endLive(me, id);
    }

    @PatchMapping
    public LiveSessionResponse update(
            @PathVariable UUID id, @AuthenticationPrincipal User me, @Valid @RequestBody UpdateLiveRequest request) {
        return liveService.updateLiveInfo(me, id, request);
    }

    @PostMapping("/join")
    public LiveSessionResponse join(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        return liveService.joinLive(me, id);
    }

    @PostMapping("/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        liveService.leaveLive(me, id);
    }

    @PostMapping("/heartbeat")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void heartbeat(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        liveService.heartbeat(me, id);
    }

    @GetMapping("/token")
    public LiveTokenResponse token(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        return liveService.getToken(me, id);
    }

    @GetMapping("/participants")
    public List<LiveParticipantResponse> participants(@PathVariable UUID id) {
        return liveService.listParticipants(id);
    }

    @PostMapping("/microphone")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void microphone(
            @PathVariable UUID id, @AuthenticationPrincipal User me, @RequestBody MicrophoneRequest request) {
        liveService.setMicrophone(me, id, request.enabled());
    }

    @PostMapping("/screen-share")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void screenShare(
            @PathVariable UUID id, @AuthenticationPrincipal User me, @RequestBody ScreenShareRequest request) {
        liveService.setScreenSharing(me, id, request.enabled());
    }

    @PostMapping("/speaking-requests")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestToSpeak(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        liveService.requestToSpeak(me, id);
    }

    @PostMapping("/speaking-requests/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelSpeakRequest(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        liveService.cancelSpeakRequest(me, id);
    }

    @GetMapping("/speaking-requests")
    public List<LiveParticipantResponse> speakingRequests(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        return liveService.listSpeakingRequests(me, id);
    }

    @PostMapping("/speaking-requests/{userId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@PathVariable UUID id, @PathVariable UUID userId, @AuthenticationPrincipal User me) {
        liveService.approveSpeaking(me, id, userId);
    }

    @PostMapping("/speaking-requests/{userId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@PathVariable UUID id, @PathVariable UUID userId, @AuthenticationPrincipal User me) {
        liveService.rejectSpeaking(me, id, userId);
    }

    @PostMapping("/participants/{userId}/demote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void demote(@PathVariable UUID id, @PathVariable UUID userId, @AuthenticationPrincipal User me) {
        liveService.demoteParticipant(me, id, userId);
    }

    @PostMapping("/participants/{userId}/mute")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void mute(@PathVariable UUID id, @PathVariable UUID userId, @AuthenticationPrincipal User me) {
        liveService.forceMute(me, id, userId);
    }

    @DeleteMapping("/participants/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal User me,
            @RequestBody(required = false) ReasonRequest request) {
        liveService.removeParticipant(me, id, userId, request != null ? request.reason() : null);
    }
}
