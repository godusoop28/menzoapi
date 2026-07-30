package com.menzo.menzo.dto.music;

public record YoutubeSearchResult(
        String videoId,
        String title,
        String channelTitle,
        String thumbnailUrl,
        Integer durationSeconds,
        boolean embeddable,
        boolean live) {
}
