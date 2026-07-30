package com.menzo.menzo.service;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Forma mínima de la respuesta de youtube/v3/search.list que YoutubeClient necesita — el resto
 * de campos que manda Google (kind, etag, pageInfo, nextPageToken, regionCode) se ignoran. */
@JsonIgnoreProperties(ignoreUnknown = true)
record YoutubeSearchResponse(List<YoutubeSearchItem> items) {
}
