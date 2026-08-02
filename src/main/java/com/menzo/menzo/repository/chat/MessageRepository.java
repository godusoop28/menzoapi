package com.menzo.menzo.repository.chat;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.menzo.menzo.domain.chat.Message;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByRoomIdOrderByCreatedAtDescIdDesc(UUID roomId, Pageable pageable);

    Optional<Message> findFirstByRoomIdOrderByCreatedAtDesc(UUID roomId);

    /** Enforce structurally, at the query level, that global staff (CURATOR/LEADER/MASTER) can
     * never touch a DIRECT (1-on-1) message — a message in a DIRECT room simply cannot be
     * returned by this query, regardless of caller. Used only by the non-author branch of
     * ChatService.deleteMessage; the author-deletes-own-message path never needs this since it
     * doesn't care about room type. */
    @Query("SELECT m FROM Message m WHERE m.id = :id AND m.room.type = com.menzo.menzo.domain.chat.RoomType.PUBLIC")
    Optional<Message> findByIdAndRoomTypePublic(@Param("id") UUID id);
}
