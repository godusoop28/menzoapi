package com.menzo.menzo.service;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Forma mínima de la respuesta de tenor/v2/search|featured que TenorClient necesita — el resto
 * de campos que manda Tenor (content_description, itemurl, url, title, ...) se ignoran. */
@JsonIgnoreProperties(ignoreUnknown = true)
record TenorRawResponse(List<TenorRawResult> results, String next) {
}
