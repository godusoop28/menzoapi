package com.menzo.menzo.dto.music;

public record SeekRequest(int positionSeconds, Long expectedVersion) {
}
