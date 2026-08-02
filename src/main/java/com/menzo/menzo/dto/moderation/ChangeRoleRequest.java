package com.menzo.menzo.dto.moderation;

import com.menzo.menzo.domain.user.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeRoleRequest(@NotNull Role role, @NotBlank @Size(max = 300) String reason) {
}
