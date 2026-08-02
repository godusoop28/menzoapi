package com.menzo.menzo.dto.gif;

import java.util.List;

public record GifSearchResponse(List<GifResult> results, String next) {
}
