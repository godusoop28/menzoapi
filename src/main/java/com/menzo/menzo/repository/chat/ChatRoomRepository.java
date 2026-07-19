package com.menzo.menzo.repository.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.menzo.menzo.domain.chat.ChatRoom;
import com.menzo.menzo.domain.chat.RoomType;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    Optional<ChatRoom> findBySlug(String slug);

    List<ChatRoom> findByType(RoomType type);

    @Query("""
            SELECT r FROM ChatRoom r
            WHERE r.type = com.menzo.menzo.domain.chat.RoomType.DIRECT
              AND EXISTS (SELECT 1 FROM RoomMember m1 WHERE m1.roomId = r.id AND m1.userId = :userA)
              AND EXISTS (SELECT 1 FROM RoomMember m2 WHERE m2.roomId = r.id AND m2.userId = :userB)
            """)
    Optional<ChatRoom> findDirectRoomBetween(@Param("userA") UUID userA, @Param("userB") UUID userB);
}
