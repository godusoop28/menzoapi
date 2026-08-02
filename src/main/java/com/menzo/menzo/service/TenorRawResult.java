package com.menzo.menzo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record TenorRawResult(String id, @JsonProperty("media_formats") TenorMediaFormats mediaFormats) {
}
