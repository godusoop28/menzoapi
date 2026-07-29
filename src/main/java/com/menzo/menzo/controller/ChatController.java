package com.menzo.menzo.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.chat.BanResponse;
import com.menzo.menzo.dto.chat.ChatRoomResponse;
import com.menzo.menzo.dto.chat.CreateRoomRequest;
import com.menzo.menzo.dto.chat.MessageResponse;
import com.menzo.menzo.dto.chat.ModerationActionRequest;
import com.menzo.menzo.dto.chat.RoomMemberResponse;
import com.menzo.menzo.dto.chat.SendMessageRequest;
import com.menzo.menzo.dto.chat.UpdateRoomRequest;
import com.menzo.menzo.dto.common.PageResponse;
import com.menzo.menzo.service.ChatService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chat/rooms")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public List<ChatRoomResponse> listRooms(@AuthenticationPrincipal User viewer) {
        return chatService.listRooms(viewer);
    }

    @GetMapping("/discover")
    public List<ChatRoomResponse> discoverRooms(
            @RequestParam(defaultValue = "recent") String sort, @AuthenticationPrincipal User viewer) {
        return chatService.listDiscoverRooms(sort, viewer);
    }

    @PatchMapping("/{id}")
    public ChatRoomResponse updateRoom(
            @PathVariable UUID id, @AuthenticationPrincipal User me, @RequestBody UpdateRoomRequest request) {
        return chatService.updateRoom(me, id, request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChatRoomResponse createRoom(@AuthenticationPrincipal User me, @Valid @RequestBody CreateRoomRequest request) {
        return chatService.createRoom(me, request);
    }

    @GetMapping("/{id}")
    public ChatRoomResponse getRoom(@PathVariable UUID id, @AuthenticationPrincipal User viewer) {
        return chatService.getRoom(id, viewer);
    }

    @PostMapping("/dm/{userId}")
    public ChatRoomResponse openDirect(@PathVariable UUID userId, @AuthenticationPrincipal User me) {
        return chatService.openDirect(me, userId);
    }

    @PostMapping("/{id}/join")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void join(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        chatService.joinRoom(me, id);
    }

    @PostMapping("/{id}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        chatService.leaveRoom(me, id);
    }

    @PutMapping("/{id}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void favorite(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        chatService.favorite(me, id);
    }

    @DeleteMapping("/{id}/favorite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfavorite(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        chatService.unfavorite(me, id);
    }

    @GetMapping("/{id}/messages")
    public PageResponse<MessageResponse> messages(
            @PathVariable UUID id,
            @PageableDefault(size = 40) Pageable pageable,
            @AuthenticationPrincipal User viewer) {
        return chatService.listMessages(id, pageable, viewer);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse sendMessage(
            @PathVariable UUID id, @AuthenticationPrincipal User me, @Valid @RequestBody SendMessageRequest request) {
        return chatService.sendMessage(me, id, request);
    }

    @GetMapping("/{id}/members")
    public List<RoomMemberResponse> members(@PathVariable UUID id) {
        return chatService.listMembers(id);
    }

    @PostMapping("/{id}/members/{userId}/promote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void promote(@PathVariable UUID id, @PathVariable UUID userId, @AuthenticationPrincipal User me) {
        chatService.promote(me, id, userId);
    }

    @PostMapping("/{id}/members/{userId}/demote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void demote(@PathVariable UUID id, @PathVariable UUID userId, @AuthenticationPrincipal User me) {
        chatService.demote(me, id, userId);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void kick(@PathVariable UUID id, @PathVariable UUID userId, @AuthenticationPrincipal User me) {
        chatService.kick(me, id, userId);
    }

    @PostMapping("/{id}/members/{userId}/ban")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void ban(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal User me,
            @RequestBody(required = false) ModerationActionRequest request) {
        chatService.ban(me, id, userId, request);
    }

    @DeleteMapping("/{id}/bans/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unban(@PathVariable UUID id, @PathVariable UUID userId, @AuthenticationPrincipal User me) {
        chatService.unban(me, id, userId);
    }

    @GetMapping("/{id}/bans")
    public List<BanResponse> bans(@PathVariable UUID id, @AuthenticationPrincipal User me) {
        return chatService.listBans(me, id);
    }

    @PostMapping("/{id}/members/{userId}/invite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void invite(@PathVariable UUID id, @PathVariable UUID userId, @AuthenticationPrincipal User me) {
        chatService.inviteMember(me, id, userId);
    }
}
