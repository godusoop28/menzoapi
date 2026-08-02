package com.menzo.menzo.repository.sticker;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.menzo.menzo.domain.sticker.StickerPack;

public interface StickerPackRepository extends JpaRepository<StickerPack, UUID> {

    @Query("""
            SELECT p FROM StickerPack p
            WHERE :query IS NULL OR lower(p.name) LIKE lower(concat('%', :query, '%'))
            ORDER BY p.createdAt DESC
            """)
    Page<StickerPack> search(@Param("query") String query, Pageable pageable);
}
