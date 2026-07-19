package com.menzo.menzo.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.chat.ChatRoom;
import com.menzo.menzo.domain.chat.Message;
import com.menzo.menzo.domain.chat.MessageType;
import com.menzo.menzo.domain.chat.RoomFavorite;
import com.menzo.menzo.domain.chat.RoomMember;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.chat.ChatRoomResponse;
import com.menzo.menzo.dto.chat.MessageResponse;
import com.menzo.menzo.dto.chat.SendMessageRequest;
import com.menzo.menzo.dto.common.PageResponse;
import com.menzo.menzo.exception.NotFoundException;
import com.menzo.menzo.repository.chat.ChatRoomRepository;
import com.menzo.menzo.repository.chat.MessageRepository;
import com.menzo.menzo.repository.chat.RoomFavoriteRepository;
import com.menzo.menzo.repository.chat.RoomMemberRepository;
import com.menzo.menzo.service.mapper.ProfileMapper;

@Service
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomFavoriteRepository roomFavoriteRepository;
    private final MessageRepository messageRepository;
    private final ProfileMapper profileMapper;

    public ChatService(
            ChatRoomRepository chatRoomRepository,
            RoomMemberRepository roomMemberRepository,
            RoomFavoriteRepository roomFavoriteRepository,
            MessageRepository messageRepository,
            ProfileMapper profileMapper) {
        this.chatRoomRepository = chatRoomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.roomFavoriteRepository = roomFavoriteRepository;
        this.messageRepository = messageRepository;
        this.profileMapper = profileMapper;
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> listRooms(User viewer) {
        return chatRoomRepository.findAll().stream()
                .map(room -> toRoomResponse(room, viewer))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatRoomResponse getRoom(UUID roomId, User viewer) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Sala no encontrada"));
        return toRoomResponse(room, viewer);
    }

    @Transactional
    public void joinRoom(User me, UUID roomId) {
        if (!chatRoomRepository.existsById(roomId)) {
            throw new NotFoundException("Sala no encontrada");
        }
        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, me.getId())) {
            roomMemberRepository.save(new RoomMember(roomId, me.getId()));
        }
    }

    @Transactional
    public void leaveRoom(User me, UUID roomId) {
        roomMemberRepository.deleteById(new RoomMember.RoomMemberId(roomId, me.getId()));
        roomFavoriteRepository.deleteByRoomIdAndUserId(roomId, me.getId());
    }

    @Transactional
    public void favorite(User me, UUID roomId) {
        if (!chatRoomRepository.existsById(roomId)) {
            throw new NotFoundException("Sala no encontrada");
        }
        if (!roomFavoriteRepository.existsByRoomIdAndUserId(roomId, me.getId())) {
            roomFavoriteRepository.save(new RoomFavorite(roomId, me.getId()));
        }
    }

    @Transactional
    public void unfavorite(User me, UUID roomId) {
        roomFavoriteRepository.deleteByRoomIdAndUserId(roomId, me.getId());
    }

    @Transactional
    public MessageResponse sendMessage(User me, UUID roomId, SendMessageRequest request) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Sala no encontrada"));

        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, me.getId())) {
            roomMemberRepository.save(new RoomMember(roomId, me.getId()));
        }

        Message message = new Message();
        message.setRoom(room);
        message.setAuthor(me);
        message.setType(MessageType.text);
        message.setBody(request.body());
        message.setImageUri(request.imageUri());
        message = messageRepository.save(message);

        return toMessageResponse(message);
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> listMessages(UUID roomId, Pageable pageable) {
        if (!chatRoomRepository.existsById(roomId)) {
            throw new NotFoundException("Sala no encontrada");
        }
        Page<Message> page = messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);
        return PageResponse.of(page, this::toMessageResponse);
    }

    private ChatRoomResponse toRoomResponse(ChatRoom room, User viewer) {
        long memberCount = roomMemberRepository.countByRoomId(room.getId());
        long onlineCount = roomMemberRepository.countOnlineMembers(room.getId());
        boolean favorite = viewer != null && roomFavoriteRepository.existsByRoomIdAndUserId(room.getId(), viewer.getId());
        boolean joined = viewer != null && roomMemberRepository.existsByRoomIdAndUserId(room.getId(), viewer.getId());

        return new ChatRoomResponse(
                room.getId(),
                room.getSlug(),
                room.getName(),
                room.getDescription(),
                room.getTopic(),
                room.getGradient(),
                room.getIcon(),
                memberCount,
                onlineCount,
                favorite,
                joined);
    }

    private MessageResponse toMessageResponse(Message message) {
        boolean isSystem = message.getType() == MessageType.system || message.getAuthor() == null;
        return new MessageResponse(
                message.getId(),
                message.getRoom().getId(),
                isSystem ? "system" : message.getAuthor().getId().toString(),
                isSystem ? null : profileMapper.toSummary(message.getAuthor()),
                message.getType().name(),
                message.getBody(),
                message.getImageUri(),
                message.getCreatedAt());
    }
}
