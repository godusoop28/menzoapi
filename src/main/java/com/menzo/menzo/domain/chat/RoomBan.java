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
@Table(name = "room_bans")
@IdClass(RoomBan.RoomBanId.class)
@Getter
@Setter
@NoArgsConstructor
public class RoomBan {

    @Id
    @Column(name = "room_id")
    private UUID roomId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "banned_by_user_id")
    private UUID bannedByUserId;

    @Column(name = "reason")
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RoomBan(UUID roomId, UUID userId, UUID bannedByUserId, String reason) {
        this.roomId = roomId;
        this.userId = userId;
        this.bannedByUserId = bannedByUserId;
        this.reason = reason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class RoomBanId implements Serializable {
        private UUID roomId;
        private UUID userId;

        public RoomBanId(UUID roomId, UUID userId) {
            this.roomId = roomId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RoomBanId that)) return false;
            return Objects.equals(roomId, that.roomId) && Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roomId, userId);
        }
    }
}
