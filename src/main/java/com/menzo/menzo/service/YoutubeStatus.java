package com.menzo.menzo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** embeddable es boolean primitivo a propósito: si Google no manda el campo, Jackson lo deja en
 * false — el mismo default que antes daba JsonNode.path("embeddable").asBoolean(false). */
@JsonIgnoreProperties(ignoreUnknown = true)
record YoutubeStatus(boolean embeddable, String privacyStatus) {
}
