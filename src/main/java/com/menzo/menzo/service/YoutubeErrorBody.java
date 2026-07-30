package com.menzo.menzo.service;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record YoutubeErrorBody(List<YoutubeErrorDetail> errors) {
}
