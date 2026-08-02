package com.menzo.menzo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Tenor manda muchos más formatos (mp4, webp, nanogif, ...) — solo se piden estos dos acá:
 * `gif` para el resultado real y `tinygif` como preview liviano en la grilla del picker. */
@JsonIgnoreProperties(ignoreUnknown = true)
record TenorMediaFormats(TenorMediaFormat gif, TenorMediaFormat tinygif) {
}
