package com.menzo.menzo.dto.community;

import java.time.LocalDate;
import java.util.UUID;

public record EventResponse(
        UUID id,
        String title,
        String description,
        LocalDate date,
        String time,
        String kind,
        long attendeeCount,
        boolean attendingByMe) {
}
