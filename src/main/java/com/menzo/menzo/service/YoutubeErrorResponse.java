package com.menzo.menzo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Forma del cuerpo de error que devuelve Google (error.errors[0].reason) — ver extractReason. */
@JsonIgnoreProperties(ignoreUnknown = true)
record YoutubeErrorResponse(YoutubeErrorBody error) {
}
