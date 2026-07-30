package com.menzo.menzo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record YoutubeSearchItem(YoutubeVideoId id) {
}
