package com.menzo.menzo.repository.chat;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.menzo.menzo.domain.chat.WallMessage;

public interface WallMessageRepository extends JpaRepository<WallMessage, UUID> {

    Page<WallMessage> findByProfileIdOrderByCreatedAtDesc(UUID profileId, Pageable pageable);
}
