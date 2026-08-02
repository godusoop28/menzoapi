package com.menzo.menzo.domain.chat;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.menzo.menzo.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MessageType type = MessageType.text;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "image_uri", columnDefinition = "text")
    private String imageUri;

    // UUID suelto, no un @ManyToOne — a propósito (ver V18__message_reply.sql): sin FK forzada,
    // si el mensaje original alguna vez se borra (no existe ese endpoint hoy, pero esto deja el
    // terreno listo) esta columna conserva el id igual, y ChatService.toReplyPreview puede armar
    // un preview "Mensaje eliminado" en vez de perder la referencia sin más.
    @Column(name = "reply_to_message_id")
    private UUID replyToMessageId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
