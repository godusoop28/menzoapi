package com.menzo.menzo.service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.chat.ChatRoom;
import com.menzo.menzo.domain.chat.Message;
import com.menzo.menzo.domain.chat.MessageType;
import com.menzo.menzo.domain.chat.RoomBan;
import com.menzo.menzo.domain.chat.RoomFavorite;
import com.menzo.menzo.domain.chat.RoomMember;
import com.menzo.menzo.domain.chat.RoomRole;
import com.menzo.menzo.domain.chat.RoomType;
import com.menzo.menzo.domain.community.NotificationCategory;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.chat.BanResponse;
import com.menzo.menzo.dto.chat.ChatRoomResponse;
import com.menzo.menzo.dto.chat.CreateRoomRequest;
import com.menzo.menzo.dto.chat.MessageResponse;
import com.menzo.menzo.dto.chat.ModerationActionRequest;
import com.menzo.menzo.dto.chat.RoomMemberResponse;
import com.menzo.menzo.dto.chat.RoomModerationEvent;
import com.menzo.menzo.dto.chat.SendMessageRequest;
import com.menzo.menzo.dto.chat.UpdateRoomRequest;
import com.menzo.menzo.dto.common.PageResponse;
import com.menzo.menzo.dto.live.LiveEvent;
import com.menzo.menzo.dto.live.LiveEventType;
import com.menzo.menzo.dto.user.UserSummary;
import com.menzo.menzo.exception.BadRequestException;
import com.menzo.menzo.exception.ConflictException;
import com.menzo.menzo.exception.ForbiddenException;
import com.menzo.menzo.exception.NotFoundException;
import com.menzo.menzo.repository.chat.ChatRoomRepository;
import com.menzo.menzo.repository.chat.MessageRepository;
import com.menzo.menzo.repository.chat.RoomBanRepository;
import com.menzo.menzo.repository.chat.RoomFavoriteRepository;
import com.menzo.menzo.repository.chat.RoomMemberRepository;
import com.menzo.menzo.repository.user.UserRepository;
import com.menzo.menzo.service.mapper.ProfileMapper;
import com.menzo.menzo.util.TextSanitizer;

@Service
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final RoomFavoriteRepository roomFavoriteRepository;
    private final RoomBanRepository roomBanRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final VoiceService voiceService;
    private final LiveService liveService;
    private final NotificationService notificationService;

    public ChatService(
            ChatRoomRepository chatRoomRepository,
            RoomMemberRepository roomMemberRepository,
            RoomFavoriteRepository roomFavoriteRepository,
            RoomBanRepository roomBanRepository,
            MessageRepository messageRepository,
            UserRepository userRepository,
            ProfileMapper profileMapper,
            SimpMessagingTemplate messagingTemplate,
            VoiceService voiceService,
            LiveService liveService,
            NotificationService notificationService) {
        this.chatRoomRepository = chatRoomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.roomFavoriteRepository = roomFavoriteRepository;
        this.roomBanRepository = roomBanRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.profileMapper = profileMapper;
        this.messagingTemplate = messagingTemplate;
        this.voiceService = voiceService;
        this.liveService = liveService;
        this.notificationService = notificationService;
    }

    /** Solo las salas a las que el usuario ya se unió (públicas o directas) — "Mis chats". */
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> listRooms(User viewer) {
        if (viewer == null) {
            return List.of();
        }
        List<ChatRoom> rooms = new ArrayList<>(chatRoomRepository.findByType(RoomType.PUBLIC).stream()
                .filter(room -> roomMemberRepository.existsByRoomIdAndUserId(room.getId(), viewer.getId()))
                .toList());
        rooms.addAll(chatRoomRepository.findDirectRoomsForUser(viewer.getId()));
        List<ChatRoomResponse> responses = rooms.stream()
                .map(room -> toRoomResponse(room, viewer))
                .collect(Collectors.toCollection(ArrayList::new));
        responses.sort(INBOX_ORDER);
        return responses;
    }

    /** "Mis chats" ordenada por actividad más reciente, no por el orden en que las trajo la
     * consulta (antes no se ordenaba nada acá — la bandeja mostraba las salas en un orden
     * esencialmente arbitrario, sin relación con qué conversación tenía el mensaje más nuevo).
     * COALESCE(lastMessage.createdAt, updatedAt, createdAt) DESC, con un desempate estable por
     * id para que dos salas con exactamente el mismo instante no floten de posición entre
     * pedidos sucesivos. */
    static final Comparator<ChatRoomResponse> INBOX_ORDER = Comparator
            .comparing(ChatService::inboxActivityInstant, Comparator.reverseOrder())
            .thenComparing(ChatRoomResponse::id);

    private static Instant inboxActivityInstant(ChatRoomResponse room) {
        if (room.lastMessage() != null && room.lastMessage().createdAt() != null) {
            return room.lastMessage().createdAt();
        }
        if (room.updatedAt() != null) {
            return room.updatedAt();
        }
        return room.createdAt();
    }

    /** Solo las salas públicas que están en vivo ahora mismo, para el carrusel "En vivo ahora" —
     * evita traer y descartar todas las salas públicas solo para filtrar por live (lo que haría
     * listDiscoverRooms), yendo directo al directorio persistido de VoiceService.
     * No es readOnly: voiceService.liveRoomIds() ejecuta un UPDATE de reconciliación
     * (endStaleSessions) que, por propagación REQUIRED, correría dentro de esta misma
     * transacción — si esta fuera readOnly, Postgres rechaza esa escritura. */
    @Transactional
    public List<ChatRoomResponse> listLiveRooms(User viewer) {
        List<UUID> liveRoomIds = voiceService.liveRoomIds();
        if (liveRoomIds.isEmpty()) {
            return List.of();
        }
        return chatRoomRepository.findAllById(liveRoomIds).stream()
                .map(room -> toRoomResponse(room, viewer))
                .toList();
    }

    /** Todas las salas públicas listadas (listed=true), unidas o no — para descubrir/explorar.
     * Una sala con listed=false (oculta por el OWNER) sigue funcionando normal para quien ya es
     * miembro, solo no aparece acá. */
    @Transactional(readOnly = true)
    public List<ChatRoomResponse> listDiscoverRooms(String sort, User viewer) {
        List<ChatRoomResponse> rooms = chatRoomRepository.findByType(RoomType.PUBLIC).stream()
                .filter(ChatRoom::isListed)
                .map(room -> toRoomResponse(room, viewer))
                .collect(Collectors.toCollection(ArrayList::new));
        Comparator<ChatRoomResponse> comparator = "popular".equalsIgnoreCase(sort)
                ? Comparator.comparingLong(ChatRoomResponse::onlineCount)
                        .thenComparingLong(ChatRoomResponse::memberCount)
                        .reversed()
                : Comparator.comparing(ChatRoomResponse::createdAt).reversed();
        rooms.sort(comparator);
        return rooms;
    }

    @Transactional(readOnly = true)
    public ChatRoomResponse getRoom(UUID roomId, User viewer) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Sala no encontrada"));
        requireCanAccess(room, viewer);
        return toRoomResponse(room, viewer);
    }

    @Transactional
    public ChatRoomResponse createRoom(User me, CreateRoomRequest request) {
        ChatRoom room = new ChatRoom();
        room.setType(RoomType.PUBLIC);
        room.setName(request.name().trim());
        room.setDescription(TextSanitizer.normalizeToNull(request.description(), 500));
        room.setTopic(request.topic() != null ? request.topic().trim() : "");
        room.setCategory(request.category() != null && !request.category().isBlank() ? request.category().trim() : null);
        if (request.gradient() != null && !request.gradient().isBlank()) {
            room.setGradient(request.gradient());
        }
        if (request.icon() != null && !request.icon().isBlank()) {
            room.setIcon(request.icon());
        }
        room.setUpdatedAt(Instant.now());
        // saveAndFlush, no save: @CreationTimestamp recién completa createdAt cuando el INSERT
        // realmente se ejecuta (al hacer flush), no al llamar a save()/persist(). Sin el flush acá,
        // toRoomResponse (unas líneas más abajo, misma transacción) leería un createdAt en null.
        room = chatRoomRepository.saveAndFlush(room);
        roomMemberRepository.save(new RoomMember(room.getId(), me.getId(), RoomRole.OWNER));
        return toRoomResponse(room, me);
    }

    @Transactional
    public void joinRoom(User me, UUID roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Sala no encontrada"));
        if (room.getType() != RoomType.PUBLIC) {
            throw new BadRequestException("No puedes unirte a una conversación privada");
        }
        if (roomBanRepository.existsByRoomIdAndUserId(roomId, me.getId())) {
            throw new ForbiddenException("Estás baneado de esta sala");
        }
        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, me.getId())) {
            if (room.getMaxMembers() != null && roomMemberRepository.countByRoomId(roomId) >= room.getMaxMembers()) {
                throw new ForbiddenException("Esta sala alcanzó su límite de miembros");
            }
            roomMemberRepository.save(new RoomMember(roomId, me.getId()));
            announceJoin(room, me);
        }
    }

    /** Anuncia en la sala que alguien se unió. Solo para salas públicas — nadie necesita ver un
     * aviso de "se unió" en una conversación directa, y el llamador ya se aseguró de que esta es
     * la primera vez que este usuario entra (para no spamear el chat en reintentos). */
    private void announceJoin(ChatRoom room, User user) {
        if (room.getType() != RoomType.PUBLIC) {
            return;
        }
        Message systemMessage = new Message();
        systemMessage.setRoom(room);
        systemMessage.setAuthor(null);
        systemMessage.setType(MessageType.system);
        systemMessage.setBody(user.getDisplayName() + " se unió a la sala.");
        systemMessage = messageRepository.saveAndFlush(systemMessage);
        broadcastMessage(room.getId(), toMessageResponse(systemMessage));
    }

    /** Empuja el mensaje a todos los que estén suscritos a la sala por WebSocket — reemplaza el
     * polling que hacían antes los dos frontends. */
    private void broadcastMessage(UUID roomId, MessageResponse response) {
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/messages", response);
    }

    /** PATCH parcial de la sala. name/description/topic/category/apariencia/allowMembersToInvite/
     * requiresApproval/maxMembers requieren OWNER o CO_HOST; listed (si aparece en "Chats
     * públicos"/descubrir) es privacidad y queda restringido al OWNER. En conversaciones DIRECT
     * (sin roles) se mantiene el comportamiento previo: cualquier miembro puede fijar portada/
     * fondo/avatar, ya que ahí no hay anfitrión. */
    @Transactional
    public ChatRoomResponse updateRoom(User me, UUID roomId, UpdateRoomRequest request) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Sala no encontrada"));
        if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, me.getId())) {
            throw new ForbiddenException("No tienes acceso a esta conversación");
        }

        boolean isDirect = room.getType() == RoomType.DIRECT;
        if (!isDirect) {
            requireOwnerOrCoHost(roomId, me);
        }

        if (request.name() != null && !request.name().isBlank()) {
            room.setName(request.name().trim());
        }
        if (request.description() != null) {
            room.setDescription(TextSanitizer.normalizeToNull(request.description(), 500));
        }
        if (request.topic() != null) {
            room.setTopic(request.topic().isBlank() ? "" : request.topic().trim());
        }
        if (request.category() != null) {
            room.setCategory(request.category().isBlank() ? null : request.category().trim());
        }
        if (request.avatarUri() != null) {
            room.setAvatarUri(request.avatarUri().isBlank() ? null : request.avatarUri());
        }
        if (request.coverUri() != null) {
            room.setCoverUri(request.coverUri().isBlank() ? null : request.coverUri());
        }
        if (request.backgroundUri() != null) {
            room.setBackgroundUri(request.backgroundUri().isBlank() ? null : request.backgroundUri());
        }
        if (request.requiresApproval() != null) {
            room.setRequiresApproval(request.requiresApproval());
        }
        if (request.allowMembersToInvite() != null) {
            room.setAllowMembersToInvite(request.allowMembersToInvite());
        }
        if (request.maxMembers() != null) {
            room.setMaxMembers(request.maxMembers());
        }
        if (request.listed() != null) {
            if (isDirect) {
                throw new BadRequestException("Una conversación directa no puede listarse");
            }
            requireOwner(roomId, me);
            room.setListed(request.listed());
        }

        room.setUpdatedAt(Instant.now());
        room = chatRoomRepository.save(room);

        boolean appearanceChanged = request.avatarUri() != null || request.coverUri() != null || request.backgroundUri() != null;
        broadcastRoomEvent(room, appearanceChanged ? "CHAT_ROOM_APPEARANCE_UPDATED" : "CHAT_ROOM_UPDATED");
        return toRoomResponse(room, me);
    }

    /** Solo el OWNER puede ceder la sala — el antiguo OWNER queda como CO_HOST (no vuelve a
     * MEMBER, para no perder de golpe la capacidad de moderar la sala que acaba de dirigir). */
    @Transactional
    public void transferOwnership(User actor, UUID roomId, UUID targetUserId) {
        getRoomOrThrow(roomId);
        requireOwner(roomId, actor);
        if (targetUserId.equals(actor.getId())) {
            throw new BadRequestException("Ya eres el anfitrión de esta sala");
        }
        RoomMember target = roomMemberRepository.findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Ese usuario no es miembro de la sala"));
        RoomMember current = roomMemberRepository.findByRoomIdAndUserId(roomId, actor.getId())
                .orElseThrow(() -> new ForbiddenException("No tienes acceso a esta sala"));

        target.setRole(RoomRole.OWNER);
        current.setRole(RoomRole.CO_HOST);
        roomMemberRepository.save(target);
        roomMemberRepository.save(current);

        User targetUser = requireUser(targetUserId);
        ChatRoom room = getRoomOrThrow(roomId);
        announceModeration(room, "ROLE_CHANGED", actor, targetUser, RoomRole.OWNER,
                targetUser.getDisplayName() + " ahora es el anfitrión de la sala.");
    }

    /** Zona peligrosa: elimina la sala por completo. Solo el OWNER. Las claves foráneas de
     * room_members/room_favorites/messages/room_bans/chat_live_sessions/live_participants ya
     * están declaradas ON DELETE CASCADE (ver migraciones), así que un solo delete alcanza. */
    @Transactional
    public void deleteRoom(User actor, UUID roomId) {
        getRoomOrThrow(roomId);
        requireOwner(roomId, actor);
        chatRoomRepository.deleteById(roomId);
    }

    /** Evento genérico de sala (no de LIVE) — /topic/rooms/{roomId}/room. Reutiliza el mismo
     * envelope LiveEvent para no introducir un tipo de mensaje más; liveSessionId siempre null
     * acá porque no aplica a un evento de sala. */
    private void broadcastRoomEvent(ChatRoom room, String type) {
        messagingTemplate.convertAndSend(
                "/topic/rooms/" + room.getId() + "/room",
                new LiveEvent(
                        UUID.randomUUID().toString(),
                        LiveEventType.valueOf(type),
                        room.getId(),
                        null,
                        Instant.now(),
                        toRoomResponse(room, null)));
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
    public ChatRoomResponse openDirect(User me, UUID otherUserId) {
        if (me.getId().equals(otherUserId)) {
            throw new BadRequestException("No puedes enviarte mensajes a ti mismo");
        }
        if (!userRepository.existsById(otherUserId)) {
            throw new NotFoundException("Usuario no encontrado");
        }

        ChatRoom room = chatRoomRepository.findDirectRoomBetween(me.getId(), otherUserId)
                .orElseGet(() -> {
                    ChatRoom created = new ChatRoom();
                    created.setType(RoomType.DIRECT);
                    created.setName(null);
                    created.setTopic("");
                    created.setGradient("community");
                    created.setIcon("chatbubbles");
                    created.setUpdatedAt(Instant.now());
                    created = chatRoomRepository.saveAndFlush(created);
                    roomMemberRepository.save(new RoomMember(created.getId(), me.getId()));
                    roomMemberRepository.save(new RoomMember(created.getId(), otherUserId));
                    return created;
                });

        return toRoomResponse(room, me);
    }

    @Transactional
    public MessageResponse sendMessage(User me, UUID roomId, SendMessageRequest request) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Sala no encontrada"));

        if (room.getType() == RoomType.DIRECT) {
            requireCanAccess(room, me);
        } else if (!roomMemberRepository.existsByRoomIdAndUserId(roomId, me.getId())) {
            if (roomBanRepository.existsByRoomIdAndUserId(roomId, me.getId())) {
                throw new ForbiddenException("Estás baneado de esta sala");
            }
            roomMemberRepository.save(new RoomMember(roomId, me.getId()));
            announceJoin(room, me);
        }

        Message message = new Message();
        message.setRoom(room);
        message.setAuthor(me);
        message.setType(MessageType.text);
        message.setBody(request.body() == null ? "" : request.body());
        message.setImageUri(request.imageUri());
        if (request.replyToMessageId() != null) {
            // Si la referencia apunta a un mensaje de OTRA sala (carrera benigna: el cliente
            // armó la respuesta y el usuario cambió de sala antes de que el envío saliera),
            // se ignora en silencio en vez de rechazar el mensaje entero — no es un error del
            // usuario, y el mensaje sigue teniendo sentido enviado sin la cita.
            Optional<Message> replyTarget = messageRepository.findById(request.replyToMessageId())
                    .filter(original -> original.getRoom().getId().equals(roomId));
            if (replyTarget.isPresent()) {
                message.setReplyToMessageId(replyTarget.get().getId());
            }
        }
        message = messageRepository.saveAndFlush(message);

        MessageResponse response = toMessageResponse(message);
        broadcastMessage(roomId, response);
        if (room.getType() == RoomType.DIRECT) {
            notifyDirectMessage(room, me, message);
        }
        return response;
    }

    /** Solo para conversaciones DIRECT (1 a 1) — un mensaje en una sala pública notificaría a
     * potencialmente decenas de miembros por cada línea escrita, algo que ninguna app de chat
     * real hace por defecto. Acá el destinatario es inequívoco: el otro miembro de la sala. */
    private void notifyDirectMessage(ChatRoom room, User sender, Message message) {
        List<RoomMember> members = roomMemberRepository.findByRoomId(room.getId());
        for (RoomMember member : members) {
            if (member.getUserId().equals(sender.getId())) continue;
            userRepository.findById(member.getUserId()).ifPresent(recipient ->
                    notificationService.create(
                            recipient,
                            NotificationCategory.mensajes,
                            sender.getDisplayName() + " te escribió",
                            message.getBody() == null || message.getBody().isBlank()
                                    ? "Envió una imagen."
                                    : truncate(message.getBody()),
                            null, room, sender, null));
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> listMessages(UUID roomId, Pageable pageable, User viewer) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new NotFoundException("Sala no encontrada"));
        requireCanAccess(room, viewer);
        Page<Message> page = messageRepository.findByRoomIdOrderByCreatedAtDescIdDesc(roomId, pageable);
        return PageResponse.of(page, this::toMessageResponse);
    }

    @Transactional(readOnly = true)
    public List<RoomMemberResponse> listMembers(UUID roomId) {
        getRoomOrThrow(roomId);
        List<RoomMember> members = new ArrayList<>(roomMemberRepository.findByRoomId(roomId));
        members.sort(Comparator.<RoomMember>comparingInt(m -> m.getRole().ordinal()).thenComparing(RoomMember::getJoinedAt));
        List<RoomMemberResponse> result = new ArrayList<>();
        for (RoomMember member : members) {
            userRepository.findById(member.getUserId()).ifPresent(user ->
                    result.add(new RoomMemberResponse(profileMapper.toSummary(user), member.getRole().name(), member.getJoinedAt())));
        }
        return result;
    }

    @Transactional
    public void promote(User actor, UUID roomId, UUID targetUserId) {
        ChatRoom room = getRoomOrThrow(roomId);
        requireOwner(roomId, actor);
        if (targetUserId.equals(actor.getId())) {
            throw new BadRequestException("No puedes cambiar tu propio rol");
        }
        RoomMember target = roomMemberRepository.findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Ese usuario no es miembro de la sala"));
        if (target.getRole() == RoomRole.CO_HOST) {
            throw new ConflictException("Ese usuario ya es coanfitrión");
        }
        target.setRole(RoomRole.CO_HOST);
        roomMemberRepository.save(target);
        User targetUser = requireUser(targetUserId);
        announceModeration(room, "ROLE_CHANGED", actor, targetUser, RoomRole.CO_HOST,
                targetUser.getDisplayName() + " ahora es coanfitrión de la sala.");
    }

    @Transactional
    public void demote(User actor, UUID roomId, UUID targetUserId) {
        ChatRoom room = getRoomOrThrow(roomId);
        requireOwner(roomId, actor);
        RoomMember target = roomMemberRepository.findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Ese usuario no es miembro de la sala"));
        if (target.getRole() != RoomRole.CO_HOST) {
            throw new BadRequestException("Ese usuario no es coanfitrión");
        }
        target.setRole(RoomRole.MEMBER);
        roomMemberRepository.save(target);
        User targetUser = requireUser(targetUserId);
        announceModeration(room, "ROLE_CHANGED", actor, targetUser, RoomRole.MEMBER,
                targetUser.getDisplayName() + " dejó de ser coanfitrión.");
    }

    @Transactional
    public void kick(User actor, UUID roomId, UUID targetUserId) {
        ChatRoom room = getRoomOrThrow(roomId);
        RoomRole actorRole = requireOwnerOrCoHost(roomId, actor);
        RoomMember target = roomMemberRepository.findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Ese usuario no es miembro de la sala"));
        requireCanModerate(actorRole, target, actor);
        roomMemberRepository.delete(target);
        User targetUser = requireUser(targetUserId);
        announceModeration(room, "KICKED", actor, targetUser, null,
                "Se expulsó a " + targetUser.getDisplayName() + " de la sala.");
    }

    @Transactional
    public void ban(User actor, UUID roomId, UUID targetUserId, ModerationActionRequest request) {
        ChatRoom room = getRoomOrThrow(roomId);
        RoomRole actorRole = requireOwnerOrCoHost(roomId, actor);
        RoomMember target = roomMemberRepository.findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Ese usuario no es miembro de la sala"));
        requireCanModerate(actorRole, target, actor);
        roomMemberRepository.delete(target);
        String reason = request != null ? request.reason() : null;
        roomBanRepository.save(new RoomBan(roomId, targetUserId, actor.getId(), reason));
        User targetUser = requireUser(targetUserId);
        announceModeration(room, "BANNED", actor, targetUser, null,
                "Se baneó a " + targetUser.getDisplayName() + " de la sala.");
    }

    @Transactional
    public void unban(User actor, UUID roomId, UUID targetUserId) {
        getRoomOrThrow(roomId);
        requireOwnerOrCoHost(roomId, actor);
        if (!roomBanRepository.existsByRoomIdAndUserId(roomId, targetUserId)) {
            throw new NotFoundException("Ese usuario no está baneado");
        }
        roomBanRepository.deleteByRoomIdAndUserId(roomId, targetUserId);
    }

    @Transactional(readOnly = true)
    public List<BanResponse> listBans(User actor, UUID roomId) {
        getRoomOrThrow(roomId);
        requireOwnerOrCoHost(roomId, actor);
        List<BanResponse> result = new ArrayList<>();
        for (RoomBan ban : roomBanRepository.findByRoomId(roomId)) {
            User user = userRepository.findById(ban.getUserId()).orElse(null);
            if (user == null) {
                continue;
            }
            UserSummary bannedBy = ban.getBannedByUserId() != null
                    ? userRepository.findById(ban.getBannedByUserId()).map(profileMapper::toSummary).orElse(null)
                    : null;
            result.add(new BanResponse(profileMapper.toSummary(user), ban.getReason(), ban.getCreatedAt(), bannedBy));
        }
        return result;
    }

    @Transactional
    public void inviteMember(User actor, UUID roomId, UUID targetUserId) {
        ChatRoom room = getRoomOrThrow(roomId);
        requireOwnerOrCoHost(roomId, actor);
        User targetUser = requireUser(targetUserId);
        if (roomBanRepository.existsByRoomIdAndUserId(roomId, targetUserId)) {
            throw new ForbiddenException("Ese usuario está baneado de esta sala");
        }
        if (roomMemberRepository.existsByRoomIdAndUserId(roomId, targetUserId)) {
            throw new ConflictException("Ese usuario ya es miembro de la sala");
        }
        roomMemberRepository.save(new RoomMember(roomId, targetUserId, RoomRole.MEMBER));
        announceModeration(room, "INVITED", actor, targetUser, null, targetUser.getDisplayName() + " se unió a la sala.");
    }

    private ChatRoom getRoomOrThrow(UUID roomId) {
        return chatRoomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Sala no encontrada"));
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private RoomRole requireOwner(UUID roomId, User actor) {
        RoomMember membership = roomMemberRepository.findByRoomIdAndUserId(roomId, actor.getId())
                .orElseThrow(() -> new ForbiddenException("No tienes acceso a esta sala"));
        if (membership.getRole() != RoomRole.OWNER) {
            throw new ForbiddenException("Solo el anfitrión puede hacer esto");
        }
        return membership.getRole();
    }

    private RoomRole requireOwnerOrCoHost(UUID roomId, User actor) {
        RoomMember membership = roomMemberRepository.findByRoomIdAndUserId(roomId, actor.getId())
                .orElseThrow(() -> new ForbiddenException("No tienes acceso a esta sala"));
        if (membership.getRole() == RoomRole.MEMBER) {
            throw new ForbiddenException("No tienes permisos de moderación en esta sala");
        }
        return membership.getRole();
    }

    /** Un coanfitrión solo puede moderar miembros comunes — nunca al anfitrión ni a otro
     * coanfitrión. Nadie puede moderarse a sí mismo. El anfitrión puede moderar a cualquiera. */
    private void requireCanModerate(RoomRole actorRole, RoomMember target, User actor) {
        if (target.getUserId().equals(actor.getId())) {
            throw new BadRequestException("No puedes moderarte a ti mismo");
        }
        if (actorRole == RoomRole.CO_HOST && target.getRole() != RoomRole.MEMBER) {
            throw new ForbiddenException("Un coanfitrión no puede moderar al anfitrión ni a otro coanfitrión");
        }
    }

    /** Deja un mensaje de sistema legible en el historial (mismo patrón que announceJoin) y además
     * publica un evento estructurado en un tópico aparte, para que el cliente del usuario afectado
     * pueda reaccionar (por ejemplo salir de la sala si lo expulsaron) sin tener que parsear texto. */
    private void announceModeration(
            ChatRoom room, String eventType, User actor, User target, RoomRole newRole, String systemText) {
        Message systemMessage = new Message();
        systemMessage.setRoom(room);
        systemMessage.setAuthor(null);
        systemMessage.setType(MessageType.system);
        systemMessage.setBody(systemText);
        systemMessage = messageRepository.saveAndFlush(systemMessage);
        broadcastMessage(room.getId(), toMessageResponse(systemMessage));

        RoomModerationEvent event = new RoomModerationEvent(
                eventType,
                room.getId(),
                target.getId(),
                target.getDisplayName(),
                actor.getId(),
                actor.getDisplayName(),
                newRole != null ? newRole.name() : null);
        messagingTemplate.convertAndSend("/topic/rooms/" + room.getId() + "/moderation", event);
    }

    private void requireCanAccess(ChatRoom room, User viewer) {
        if (room.getType() != RoomType.DIRECT) {
            return;
        }
        if (viewer == null || !roomMemberRepository.existsByRoomIdAndUserId(room.getId(), viewer.getId())) {
            throw new ForbiddenException("No tienes acceso a esta conversación");
        }
    }

    private ChatRoomResponse toRoomResponse(ChatRoom room, User viewer) {
        long memberCount = roomMemberRepository.countByRoomId(room.getId());
        long onlineCount = roomMemberRepository.countOnlineMembers(room.getId());
        boolean favorite = viewer != null && roomFavoriteRepository.existsByRoomIdAndUserId(room.getId(), viewer.getId());
        String role = viewer == null ? null : roomMemberRepository.findByRoomIdAndUserId(room.getId(), viewer.getId())
                .map(rm -> rm.getRole().name())
                .orElse(null);
        boolean joined = role != null;
        boolean live = voiceService.isLive(room.getId());

        UserSummary peer = null;
        if (room.getType() == RoomType.DIRECT && viewer != null) {
            peer = roomMemberRepository.findOtherMemberUserId(room.getId(), viewer.getId())
                    .flatMap(userRepository::findById)
                    .map(profileMapper::toSummary)
                    .orElse(null);
        }

        ChatRoomResponse.LastMessage lastMessage = messageRepository.findFirstByRoomIdOrderByCreatedAtDesc(room.getId())
                .map(this::toLastMessage)
                .orElse(null);

        ChatRoomResponse.LiveSummary liveSummary = live ? liveService.getSummary(room.getId()) : null;

        return new ChatRoomResponse(
                room.getId(),
                room.getSlug(),
                room.getName(),
                room.getDescription(),
                room.getTopic(),
                room.getGradient(),
                room.getIcon(),
                room.getType().name(),
                room.getAvatarUri(),
                room.getCoverUri(),
                room.getBackgroundUri(),
                room.getCategory(),
                room.getMaxMembers(),
                room.isRequiresApproval(),
                room.isAllowMembersToInvite(),
                room.isListed(),
                peer,
                memberCount,
                onlineCount,
                favorite,
                joined,
                role,
                live,
                liveSummary,
                room.getCreatedAt(),
                room.getUpdatedAt(),
                lastMessage);
    }

    private ChatRoomResponse.LastMessage toLastMessage(Message message) {
        boolean isSystem = message.getType() == MessageType.system || message.getAuthor() == null;
        return new ChatRoomResponse.LastMessage(
                message.getBody(),
                message.getImageUri() != null && !message.getImageUri().isBlank(),
                isSystem ? "system" : message.getAuthor().getId().toString(),
                message.getCreatedAt());
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
                formatInstant(message.getCreatedAt()),
                toReplyPreview(message.getReplyToMessageId()));
    }

    /** Sin FK forzada (ver V18__message_reply.sql), así que "el id no resuelve" es una señal
     * real y distinguible de "nunca fue una respuesta" (`replyToMessageId == null`) — no existe
     * ningún endpoint de borrado de mensajes hoy, pero esto deja el terreno correcto para cuando
     * lo haya: el id sobrevive y el preview cae a "Mensaje eliminado" en vez de perderse. */
    private MessageResponse.ReplyPreview toReplyPreview(UUID replyToMessageId) {
        if (replyToMessageId == null) return null;
        return messageRepository.findById(replyToMessageId)
                .map(original -> {
                    boolean originalIsSystem = original.getType() == MessageType.system || original.getAuthor() == null;
                    String authorName = originalIsSystem ? "Menzo" : original.getAuthor().getDisplayName();
                    String bodyPreview = original.getImageUri() != null && !original.getImageUri().isBlank()
                            && (original.getBody() == null || original.getBody().isBlank())
                            ? "Imagen"
                            : truncate(original.getBody());
                    return new MessageResponse.ReplyPreview(original.getId(), authorName, bodyPreview, false);
                })
                .orElseGet(() -> new MessageResponse.ReplyPreview(replyToMessageId, null, null, true));
    }

    /** Trunca a milisegundos antes de convertir a String — Hermes (el motor JS de React Native)
     * no parsea de forma confiable ISO 8601 con más de 3 decimales. Se hace acá, a mano, en vez
     * de confiar en que el ObjectMapper serialice Instant así globalmente (ver JacksonConfig). */
    private static String formatInstant(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant.truncatedTo(ChronoUnit.MILLIS));
    }

    private static String truncate(String text) {
        if (text == null) return "";
        return text.length() > 140 ? text.substring(0, 140) + "…" : text;
    }
}
