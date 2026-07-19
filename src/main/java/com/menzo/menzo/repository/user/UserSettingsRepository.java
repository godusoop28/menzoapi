package com.menzo.menzo.repository.user;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.menzo.menzo.domain.user.UserSettings;

public interface UserSettingsRepository extends JpaRepository<UserSettings, UUID> {
}
