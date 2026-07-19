package com.menzo.menzo.domain.chat;

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
@Table(name = "room_members")
@IdClass(RoomMember.RoomMemberId.class)
@Getter
@Setter
@NoArgsConstructor
public class RoomMember {

    @Id
    @Column(name = "room_id")
    private UUID roomId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    public RoomMember(UUID roomId, UUID userId) {
        this.roomId = roomId;
        this.userId = userId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RoomMemberId implements Serializable {
        private UUID roomId;
        private UUID userId;

        public RoomMemberId(UUID roomId, UUID userId) {
            this.roomId = roomId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RoomMemberId that)) return false;
            return Objects.equals(roomId, that.roomId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roomId, userId);
        }
    }
}
