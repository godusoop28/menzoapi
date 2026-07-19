package com.menzo.menzo.repository.chat;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.menzo.menzo.domain.chat.Message;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByRoomIdOrderByCreatedAtDesc(UUID roomId, Pageable pageable);
}
