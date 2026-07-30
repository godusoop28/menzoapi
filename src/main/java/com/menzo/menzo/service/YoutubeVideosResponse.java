package com.menzo.menzo.service;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Forma mínima de la respuesta de youtube/v3/videos.list que YoutubeClient necesita. */
@JsonIgnoreProperties(ignoreUnknown = true)
record YoutubeVideosResponse(List<YoutubeVideoItem> items) {
}
