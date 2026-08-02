package com.menzo.menzo.domain.post;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.menzo.menzo.domain.community.CommunityEvent;
import com.menzo.menzo.domain.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "posts")
@Getter
@Setter
@NoArgsConstructor
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostType type;

    @Column(length = 150)
    private String title;

    // Para posts de tipo text/image: excerpt de solo texto derivado server-side de `blocks`
    // (concatenación de paragraph/heading, ver PostService) — NO es lo que el usuario tipeó
    // directamente, así que sigue alimentando búsqueda (PostRepository.search) y los previews de
    // notificación sin tocar ese código. Para poll/question/event sigue siendo el body crudo de
    // siempre — esos tipos no usan `blocks`.
    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "image_uri", columnDefinition = "text")
    private String imageUri;

    // Contenido real de un post text/image — lista ordenada de bloques (párrafo/título/imagen/
    // gif/separador, ver PostBlock). Default '[]' para que cada fila ya existente antes de esta
    // columna siga rindiendo exactamente igual que antes (los clientes caen al `body`/`imageUri`
    // planos cuando `blocks` viene vacío — ver PostBlockRenderer en ambos clientes).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<PostBlock> blocks = new ArrayList<>();

    @Column(name = "abstract_visual_preset", length = 30)
    private String abstractVisualPreset;

    @Column(name = "abstract_visual_caption", length = 255)
    private String abstractVisualCaption;

    @Column(length = 30)
    private String gradient;

    @Column(nullable = false)
    private boolean featured = false;

    // Acción de CURATOR+ ("ocultar", no "borrar") — distinto de eliminar por completo. Los
    // listados de feed/búsqueda/muro filtran hidden = false; getPost (fetch directo por id) NO
    // filtra, para que el autor o el staff que lo está revisando lo sigan viendo.
    @Column(nullable = false)
    private boolean hidden = false;

    @Column(name = "comment_count", nullable = false)
    private int commentCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private CommunityEvent event;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag", length = 40)
    private Set<String> tags = new HashSet<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<PollOption> pollOptions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
