package com.menzo.menzo.repository.community;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.menzo.menzo.domain.community.CommunityEvent;

public interface CommunityEventRepository extends JpaRepository<CommunityEvent, UUID> {

    List<CommunityEvent> findAllByOrderByDateAscTimeAsc();
}
