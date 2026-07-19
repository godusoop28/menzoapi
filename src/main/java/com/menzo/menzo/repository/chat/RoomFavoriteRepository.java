package com.menzo.menzo.repository.chat;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.chat.RoomFavorite;

public interface RoomFavoriteRepository extends JpaRepository<RoomFavorite, RoomFavorite.RoomFavoriteId> {

    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);

    List<RoomFavorite> findByUserId(UUID userId);

    @Transactional
    void deleteByRoomIdAndUserId(UUID roomId, UUID userId);
}
