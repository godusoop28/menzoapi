package com.menzo.menzo.repository.chat;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.menzo.menzo.domain.chat.LiveMusicSession;

public interface LiveMusicSessionRepository extends JpaRepository<LiveMusicSession, UUID> {

    Optional<LiveMusicSession> findByLiveSessionId(UUID liveSessionId);
}
