package com.menzo.menzo.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;

import com.menzo.menzo.domain.user.Badge;

public interface BadgeRepository extends JpaRepository<Badge, String> {
}
