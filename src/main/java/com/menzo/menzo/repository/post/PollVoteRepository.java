package com.menzo.menzo.repository.post;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.post.PollVote;

public interface PollVoteRepository extends JpaRepository<PollVote, UUID> {

    Optional<PollVote> findByPostIdAndUserId(UUID postId, UUID userId);

    List<PollVote> findByPostId(UUID postId);

    List<PollVote> findByOptionIdIn(List<UUID> optionIds);

    @Transactional
    void deleteByPostIdAndUserId(UUID postId, UUID userId);
}
