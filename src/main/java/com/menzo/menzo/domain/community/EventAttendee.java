package com.menzo.menzo.domain.community;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "event_attendees")
@IdClass(EventAttendee.EventAttendeeId.class)
@Getter
@Setter
@NoArgsConstructor
public class EventAttendee {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    public EventAttendee(UUID eventId, UUID userId) {
        this.eventId = eventId;
        this.userId = userId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EventAttendeeId implements Serializable {
        private UUID eventId;
        private UUID userId;

        public EventAttendeeId(UUID eventId, UUID userId) {
            this.eventId = eventId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof EventAttendeeId that)) return false;
            return Objects.equals(eventId, that.eventId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventId, userId);
        }
    }
}
