package com.menzo.menzo.repository.community;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.community.EventAttendee;

public interface EventAttendeeRepository extends JpaRepository<EventAttendee, EventAttendee.EventAttendeeId> {

    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);

    List<EventAttendee> findByEventId(UUID eventId);

    @Transactional
    void deleteByEventIdAndUserId(UUID eventId, UUID userId);
}
