package com.menzo.menzo.dto.activity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecentSearchRequest(@NotBlank @Size(max = 140) String query) {
}
