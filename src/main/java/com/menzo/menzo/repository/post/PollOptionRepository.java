package com.menzo.menzo.repository.post;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.menzo.menzo.domain.post.PollOption;

public interface PollOptionRepository extends JpaRepository<PollOption, UUID> {

    List<PollOption> findByPostIdOrderBySortOrderAsc(UUID postId);
}
