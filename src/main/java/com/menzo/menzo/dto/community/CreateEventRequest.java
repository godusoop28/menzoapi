package com.menzo.menzo.dto.community;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEventRequest(
        @NotBlank @Size(max = 150) String title,
        @NotBlank String description,
        @NotNull LocalDate date,
        @NotBlank @Size(max = 10) String time,
        @NotBlank @Size(max = 50) String kind) {
}
