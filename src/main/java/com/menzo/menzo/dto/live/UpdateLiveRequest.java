package com.menzo.menzo.dto.live;

import jakarta.validation.constraints.Size;

/** PATCH parcial, mismo convenio que UpdateRoomRequest: null = no cambiar, "" = limpiar. */
public record UpdateLiveRequest(
        @Size(max = 100) String title,
        @Size(max = 500) String description,
        @Size(max = 300) String announcement) {
}
