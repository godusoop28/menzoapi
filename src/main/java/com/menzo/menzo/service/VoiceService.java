package com.menzo.menzo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.config.AgoraProperties;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.user.UserSummary;
import com.menzo.menzo.dto.voice.VoiceParticipantsResponse;
import com.menzo.menzo.dto.voice.VoiceTokenResponse;
import com.menzo.menzo.exception.ForbiddenException;
import com.menzo.menzo.exception.NotFoundException;
import com.menzo.menzo.repository.chat.ChatRoomRepository;
import com.menzo.menzo.repository.chat.RoomMemberRepository;
import com.menzo.menzo.repository.user.UserRepository;
import com.menzo.menzo.security.agora.RtcTokenBuilder2;
import com.menzo.menzo.service.mapper.ProfileMapper;

/**
 * Presencia de voz en tiempo real: quién está en la llamada de cada sala ahora mismo. No se
 * persiste — es estado efímero por naturaleza (una instancia de Render, se resetea en cada
 * deploy sin que eso importe) así que alcanza con memoria en vez de una tabla/migración.
 */
@Service
public class VoiceService {

    private static final int TOKEN_EXPIRE_SECONDS = 3600;

    private final AgoraProperties agoraProperties;
    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;
    private final Map<UUID, Set<UUID>> participantsByRoom = new ConcurrentHashMap<>();

    public VoiceService(
            AgoraProperties agoraProperties,
            ChatRoomRepository chatRoomRepository,
            RoomMemberRepository roomMemberRepository,
            UserRepository userRepository,
            ProfileMapper profileMapper) {
        this.agoraProperties = agoraProperties;
        this.chatRoomRepository = chatRoomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.userRepository = userRepository;
        this.profileMapper = profileMapper;
    }

    @Transactional(readOnly = true)
    public VoiceTokenResponse token(User me, UUID roomId) {
        requireMember(roomId, me);
        String channelName = "room-" + roomId;
        String uid = me.getId().toString();
        String token = new RtcTokenBuilder2().buildTokenWithUserAccount(
                agoraProperties.getAppId(),
                agoraProperties.getAppCertificate(),
                channelName,
                uid,
                RtcTokenBuilder2.Role.ROLE_PUBLISHER,
                TOKEN_EXPIRE_SECONDS,
                TOKEN_EXPIRE_SECONDS);
        return new VoiceTokenResponse(agoraProperties.getAppId(), channelName, token, uid);
    }

    @Transactional(readOnly = true)
    public VoiceParticipantsResponse join(User me, UUID roomId) {
        requireMember(roomId, me);
        participantsByRoom.computeIfAbsent(roomId, id -> ConcurrentHashMap.newKeySet()).add(me.getId());
        return participants(roomId);
    }

    @Transactional(readOnly = true)
    public VoiceParticipantsResponse leave(User me, UUID roomId) {
        Set<UUID> current = participantsByRoom.get(roomId);
        if (current != null) {
            current.remove(me.getId());
            if (current.isEmpty()) {
                participantsByRoom.remove(roomId);
            }
        }
        return participants(roomId);
    }

    @Transactional(readOnly = true)
    public VoiceParticipantsResponse participants(UUID roomId) {
        Set<UUID> ids = participantsByRoom.getOrDefault(roomId, Set.of());
        List<UserSummary> summaries = new ArrayList<>();
        for (UUID userId : ids) {
            userRepository.findById(userId).ifPresent(user -> summaries.add(profileMapper.toSummary(user)));
        }
        return new VoiceParticipantsResponse(summaries);
    }

    /** Usado por ChatService para marcar una sala como EN VIVO en los listados — la presencia de
     * voz es la única fuente de verdad, no hay una bandera "live" separada que se pueda desincronizar. */
    public boolean isLive(UUID roomId) {
        return !participantsByRoom.getOrDefault(roomId, Set.of()).isEmpty();
    }

    private void requireMember(UUID roomId, User me) {
        if (!chatRoomRepository.existsById(roomId)) {
            throw new NotFoundException("Sala no encontrada");
        }
        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, me.getId())) {
            throw new ForbiddenException("Tenés que unirte a la sala antes de entrar a la voz");
        }
    }
}
