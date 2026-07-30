package com.menzo.menzo.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record YoutubeErrorDetail(String reason) {
}
