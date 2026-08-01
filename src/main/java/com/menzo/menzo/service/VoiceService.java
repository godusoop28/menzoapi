package com.menzo.menzo.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.chat.ChatLiveSession;
import com.menzo.menzo.domain.chat.LiveSessionStatus;
import com.menzo.menzo.repository.chat.ChatLiveSessionRepository;

/**
 * Fase 18 (retiro del flujo de voz legado): este servicio llegó a tener `token()`/`join()`/
 * `leave()`/`participants()` respaldados por un mapa en memoria (`participantsByRoom`) y su
 * propio `VoiceController` en `/api/chat/rooms/{id}/voice/*` — pensado en su momento para
 * menzomovil. Se verificó (grep real sobre menzomovil y menzoweb, no solo comentarios del
 * código, que estaban desactualizados) que NINGÚN cliente actual llama esos endpoints: la app
 * móvil usa exclusivamente `/api/chat/rooms/{id}/live/*` (ver LiveService, que persiste
 * `LiveParticipant` con roles/moderación reales) y la web tiene un `voiceApi` definido pero
 * jamás invocado. Mantener las escrituras vivas era el riesgo real señalado en la auditoría: un
 * `/voice/join`/`leave` de un cliente viejo podía pisar `participantCount` de una fila
 * `chat_live_sessions` que en los hechos estaba siendo gobernada por LiveService, dos fuentes de
 * verdad escribiendo la misma sesión. Se eliminaron `VoiceController` y esos métodos; lo único
 * que sigue siendo necesario es el DIRECTORIO de "qué salas están en vivo ahora" para
 * listados/carrusel (`ChatService.listLiveRooms`/`toRoomResponse`), que sigue viviendo acá por
 * compatibilidad de nombre pero ya no tiene ninguna escritura propia — solo lee
 * `chat_live_sessions`, la misma tabla que LiveService mantiene con su propio heartbeat.
 */
@Service
public class VoiceService {

    // 3x el intervalo de heartbeat de LiveNotifier (15s, ver LiveService.heartbeat) — suficiente
    // margen para tolerar un tick perdido por una red lenta sin dar de baja una sesión que sigue
    // genuinamente en uso.
    private static final long LIVE_HEARTBEAT_TTL_SECONDS = 45;

    private final ChatLiveSessionRepository chatLiveSessionRepository;

    public VoiceService(ChatLiveSessionRepository chatLiveSessionRepository) {
        this.chatLiveSessionRepository = chatLiveSessionRepository;
    }

    /** Usado por ChatService para marcar una sala como EN VIVO en los listados. Filtra por
     * heartbeat reciente, así una sesión huérfana (proceso reiniciado, cliente caído sin avisar)
     * deja de contar sola, sin necesitar tocarla primero. */
    @Transactional(readOnly = true)
    public boolean isLive(UUID roomId) {
        return chatLiveSessionRepository.existsByRoomIdAndStatusAndLastHeartbeatAtAfter(
                roomId, LiveSessionStatus.ACTIVE, Instant.now().minusSeconds(LIVE_HEARTBEAT_TTL_SECONDS));
    }

    /** Ids de las salas realmente en vivo ahora mismo, para el directorio/carrusel — corre la
     * reconciliación perezosa primero (cierra sesiones ACTIVE con heartbeat viejo) así la tabla
     * no acumula filas huérfanas indefinidamente. */
    @Transactional
    public List<UUID> liveRoomIds() {
        Instant cutoff = Instant.now().minusSeconds(LIVE_HEARTBEAT_TTL_SECONDS);
        chatLiveSessionRepository.endStaleSessions(cutoff, Instant.now(), LiveSessionStatus.ACTIVE, LiveSessionStatus.ENDED);
        return chatLiveSessionRepository
                .findByStatusAndLastHeartbeatAtAfterOrderByStartedAtDesc(LiveSessionStatus.ACTIVE, cutoff)
                .stream()
                .map(ChatLiveSession::getRoomId)
                .toList();
    }
}
