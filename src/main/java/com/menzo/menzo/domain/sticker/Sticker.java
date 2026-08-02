package com.menzo.menzo.domain.sticker;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "stickers")
@Getter
@Setter
@NoArgsConstructor
public class Sticker {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pack_id", nullable = false)
    private StickerPack pack;

    @Column(name = "image_url", nullable = false, columnDefinition = "text")
    private String imageUrl;

    // La portada del pack se deriva del sticker con menor sortOrder — no hay un campo aparte.
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
