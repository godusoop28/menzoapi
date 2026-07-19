package com.menzo.menzo.repository.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.menzo.menzo.domain.chat.RoomMember;

public interface RoomMemberRepository extends JpaRepository<RoomMember, RoomMember.RoomMemberId> {

    boolean existsByRoomIdAndUserId(UUID roomId, UUID userId);

    long countByRoomId(UUID roomId);

    List<RoomMember> findByRoomId(UUID roomId);

    List<RoomMember> findByUserId(UUID userId);

    @org.springframework.data.jpa.repository.Query(
            "SELECT m.userId FROM RoomMember m WHERE m.roomId = :roomId AND m.userId <> :excludeUserId")
    Optional<UUID> findOtherMemberUserId(
            @org.springframework.data.repository.query.Param("roomId") UUID roomId,
            @org.springframework.data.repository.query.Param("excludeUserId") UUID excludeUserId);

    @org.springframework.data.jpa.repository.Query("""
            SELECT COUNT(rm) FROM RoomMember rm
            JOIN User u ON u.id = rm.userId
            WHERE rm.roomId = :roomId AND u.online = true
            """)
    long countOnlineMembers(@org.springframework.data.repository.query.Param("roomId") UUID roomId);
}
