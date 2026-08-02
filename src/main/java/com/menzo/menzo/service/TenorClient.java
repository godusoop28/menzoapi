package com.menzo.menzo.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.menzo.menzo.config.TenorProperties;
import com.menzo.menzo.dto.gif.GifResult;
import com.menzo.menzo.dto.gif.GifSearchResponse;
import com.menzo.menzo.exception.ApiException;

/**
 * Único punto de contacto con la API de Tenor en todo el backend — mismo principio que
 * YoutubeClient: la API key nunca sale de acá (no se devuelve en ningún DTO, no se loguea), y los
 * clientes (web/mobile) solo le pegan a TenorController, nunca directo a Tenor.
 */
@Service
public class TenorClient {

    private static final Logger log = LoggerFactory.getLogger(TenorClient.class);

    private static final String SEARCH_URL = "https://tenor.googleapis.com/v2/search";
    private static final String FEATURED_URL = "https://tenor.googleapis.com/v2/featured";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 8000;

    private final TenorProperties properties;
    private final RestClient restClient;

    public TenorClient(TenorProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restClient = RestClient.builder().requestFactory(factory).build();
        // Nunca se loguea el valor de la key — solo si está presente o no.
        if (properties.getApiKey().isBlank()) {
            log.warn("Tenor integration disabled: missing API key");
        } else {
            log.info("Tenor integration enabled");
        }
    }

    /** query no vacío ya validado por TenorController — acá solo se asume no-blank. */
    public GifSearchResponse search(String query, String pos) {
        requireConfigured();
        TenorRawResponse raw;
        try {
            raw = restClient.get()
                    .uri(uriBuilder -> {
                        var b = uriBuilder
                                .scheme("https").host("tenor.googleapis.com").path("/v2/search")
                                .queryParam("q", query)
                                .queryParam("key", properties.getApiKey())
                                .queryParam("client_key", properties.getClientKey())
                                .queryParam("limit", properties.getLimit())
                                .queryParam("media_filter", "gif");
                        if (pos != null && !pos.isBlank()) {
                            b = b.queryParam("pos", pos);
                        }
                        return b.build();
                    })
                    .retrieve()
                    .body(TenorRawResponse.class);
        } catch (RestClientException e) {
            throw mapError("search", e);
        }
        return trim(raw);
    }

    public GifSearchResponse trending(String pos) {
        requireConfigured();
        TenorRawResponse raw;
        try {
            raw = restClient.get()
                    .uri(uriBuilder -> {
                        var b = uriBuilder
                                .scheme("https").host("tenor.googleapis.com").path("/v2/featured")
                                .queryParam("key", properties.getApiKey())
                                .queryParam("client_key", properties.getClientKey())
                                .queryParam("limit", properties.getLimit())
                                .queryParam("media_filter", "gif");
                        if (pos != null && !pos.isBlank()) {
                            b = b.queryParam("pos", pos);
                        }
                        return b.build();
                    })
                    .retrieve()
                    .body(TenorRawResponse.class);
        } catch (RestClientException e) {
            throw mapError("trending", e);
        }
        return trim(raw);
    }

    private void requireConfigured() {
        if (properties.getApiKey().isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "TENOR_NOT_CONFIGURED",
                    "El buscador de GIFs no está disponible en este momento.");
        }
    }

    private GifSearchResponse trim(TenorRawResponse raw) {
        if (raw == null || raw.results() == null) {
            return new GifSearchResponse(List.of(), null);
        }
        List<GifResult> results = raw.results().stream()
                .filter(r -> r.mediaFormats() != null && r.mediaFormats().gif() != null)
                .map(r -> {
                    TenorMediaFormat gif = r.mediaFormats().gif();
                    TenorMediaFormat preview = r.mediaFormats().tinygif() != null
                            ? r.mediaFormats().tinygif()
                            : gif;
                    List<Integer> dims = gif.dims();
                    Integer width = dims != null && dims.size() > 0 ? dims.get(0) : null;
                    Integer height = dims != null && dims.size() > 1 ? dims.get(1) : null;
                    return new GifResult(r.id(), gif.url(), preview.url(), width, height);
                })
                .toList();
        return new GifSearchResponse(results, raw.next());
    }

    /** Cualquier falla de Tenor (red, 4xx, 5xx) se traduce a un mensaje genérico — nunca se
     * propaga el motivo interno ni nada de la respuesta cruda de Tenor al cliente. */
    private static ApiException mapError(String stage, RestClientException e) {
        log.warn("Tenor {} failed: {}", stage, e.getMessage());
        return new ApiException(HttpStatus.BAD_GATEWAY, "TENOR_UNAVAILABLE",
                "No pudimos buscar GIFs en este momento.");
    }
}
