package com.menzo.menzo.service;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** liveStreamingDetails solo importa por su presencia (video en vivo) — nunca se leen sus campos,
 * así que se mapea a un Map genérico en vez de declarar toda su forma. */
@JsonIgnoreProperties(ignoreUnknown = true)
record YoutubeVideoItem(
        String id,
        YoutubeSnippet snippet,
        YoutubeContentDetails contentDetails,
        YoutubeStatus status,
        Map<String, Object> liveStreamingDetails) {
}
