package com.menzo.menzo.repository.chat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.menzo.menzo.domain.chat.ChatLiveSession;
import com.menzo.menzo.domain.chat.LiveSessionStatus;

public interface ChatLiveSessionRepository extends JpaRepository<ChatLiveSession, UUID> {

    Optional<ChatLiveSession> findByRoomIdAndStatus(UUID roomId, LiveSessionStatus status);

    boolean existsByRoomIdAndStatusAndLastHeartbeatAtAfter(UUID roomId, LiveSessionStatus status, Instant cutoff);

    List<ChatLiveSession> findByStatusAndLastHeartbeatAtAfterOrderByStartedAtDesc(LiveSessionStatus status, Instant cutoff);

    /** Reconciliación perezosa: una sesión ACTIVE cuyo heartbeat quedó viejo (el proceso se
     * reinició, o el cliente se cayó sin llamar a /voice/leave) se cierra acá, en cada lectura
     * del directorio — sin necesitar un scheduler aparte. Enums como parámetros bind (no como
     * literal calificado en el JPQL) para no depender de si el proveedor JPA de turno soporta
     * esa sintaxis exacta. */
    @Modifying
    @Query("UPDATE ChatLiveSession s SET s.status = :endedStatus, s.endedAt = :now "
            + "WHERE s.status = :activeStatus AND s.lastHeartbeatAt < :cutoff")
    int endStaleSessions(
            @Param("cutoff") Instant cutoff,
            @Param("now") Instant now,
            @Param("activeStatus") LiveSessionStatus activeStatus,
            @Param("endedStatus") LiveSessionStatus endedStatus);
}
