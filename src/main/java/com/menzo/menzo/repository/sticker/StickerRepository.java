package com.menzo.menzo.repository.sticker;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.menzo.menzo.domain.sticker.Sticker;

public interface StickerRepository extends JpaRepository<Sticker, UUID> {

    List<Sticker> findByPackIdOrderBySortOrderAsc(UUID packId);

    void deleteByPackId(UUID packId);
}
