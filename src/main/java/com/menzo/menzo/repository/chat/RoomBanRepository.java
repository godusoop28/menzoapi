package com.menzo.menzo.repository.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.chat.RoomBan;

public interface RoomBanRepository extends JpaRepository<RoomBan, RoomBan.RoomBanId> {

    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);

    Optional<RoomBan> findByRoomIdAndUserId(UUID roomId, UUID userId);

    List<RoomBan> findByRoomId(UUID roomId);

    @Transactional
    void deleteByRoomIdAndUserId(UUID roomId, UUID userId);
}
