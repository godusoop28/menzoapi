package com.menzo.menzo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** "default" es palabra reservada en Java — se remapea a defaultThumbnail vía @JsonProperty. */
@JsonIgnoreProperties(ignoreUnknown = true)
record YoutubeThumbnails(
        @JsonProperty("default") YoutubeThumbnail defaultThumbnail,
        YoutubeThumbnail medium,
        YoutubeThumbnail high) {
}
