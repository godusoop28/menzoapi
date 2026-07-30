package com.menzo.menzo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record YoutubeSnippet(String title, String channelTitle, String liveBroadcastContent, YoutubeThumbnails thumbnails) {
}
