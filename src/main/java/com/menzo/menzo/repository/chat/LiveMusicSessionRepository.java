package com.menzo.menzo.repository.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.menzo.menzo.domain.chat.LiveMusicSession;
import com.menzo.menzo.domain.chat.MusicSessionStatus;

public interface LiveMusicSessionRepository extends JpaRepository<LiveMusicSession, UUID> {

    Optional<LiveMusicSession> findByLiveSessionId(UUID liveSessionId);

    /** Usado por MusicAutoAdvanceScheduler cada 5s — antes traía la tabla completa con findAll()
     * y filtraba en Java; con salas viejas/terminadas acumulándose esto crecía sin límite. Solo
     * las sesiones PLAYING pueden necesitar avanzar solas. */
    List<LiveMusicSession> findByStatus(MusicSessionStatus status);
}
