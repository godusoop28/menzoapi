package com.menzo.menzo.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.voice.VoiceParticipantsResponse;
import com.menzo.menzo.dto.voice.VoiceTokenResponse;
import com.menzo.menzo.service.VoiceService;

@RestController
@RequestMapping("/api/chat/rooms/{id}/voice")
public class VoiceController {

    private final VoiceService voiceService;

    public VoiceController(VoiceService voiceService) {
        this.voiceService = voiceService;
    }

    @PostMapping("/token")
    public VoiceTokenResponse token(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        return voiceService.token(me, id);
    }

    @PostMapping("/join")
    public VoiceParticipantsResponse join(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        return voiceService.join(me, id);
    }

    @PostMapping("/leave")
    public VoiceParticipantsResponse leave(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        return voiceService.leave(me, id);
    }

    @GetMapping("/participants")
    public VoiceParticipantsResponse participants(@PathVariable UUID id) {
        return voiceService.participants(id);
    }
}
