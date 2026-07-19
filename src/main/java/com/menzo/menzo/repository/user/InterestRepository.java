package com.menzo.menzo.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;

import com.menzo.menzo.domain.user.Interest;

public interface InterestRepository extends JpaRepository<Interest, String> {
}
