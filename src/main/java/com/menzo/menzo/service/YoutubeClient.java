package com.menzo.menzo.service;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.menzo.menzo.config.YoutubeProperties;
import com.menzo.menzo.dto.music.YoutubeSearchResult;
import com.menzo.menzo.exception.ApiException;
import com.menzo.menzo.exception.BadRequestException;

/**
 * Único punto de contacto con YouTube Data API v3 en todo el backend. La API key nunca sale de
 * acá: no se devuelve en ningún DTO, no se loguea, y cualquier error de Google se traduce a un
 * mensaje genérico antes de propagarse (ver mapError) — el cliente jamás ve la respuesta cruda
 * de Google ni el motivo interno de una falla.
 */
@Service
public class YoutubeClient {

    private static final Logger log = LoggerFactory.getLogger(YoutubeClient.class);

    private static final String SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";
    private static final String VIDEOS_URL = "https://www.googleapis.com/youtube/v3/videos";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 8000;

    /** Cubre youtube.com/watch?v=, youtu.be/, youtube.com/shorts/ y youtube.com/embed/. */
    private static final Pattern YOUTUBE_LINK = Pattern.compile(
            "(?:youtube\\.com/(?:watch\\?v=|shorts/|embed/)|youtu\\.be/)([a-zA-Z0-9_-]{11})");

    private final YoutubeProperties properties;
    private final YoutubeSearchCache cache;
    private final YoutubeRateLimiter rateLimiter;
    private final RestClient restClient;

    public YoutubeClient(YoutubeProperties properties, YoutubeSearchCache cache, YoutubeRateLimiter rateLimiter) {
        this.properties = properties;
        this.cache = cache;
        this.rateLimiter = rateLimiter;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restClient = RestClient.builder().requestFactory(factory).build();
        // Nunca se loguea el valor de la key — solo si está presente o no (ver sección 3 del pedido).
        if (properties.getApiKey().isBlank()) {
            log.warn("YouTube integration disabled: missing API key");
        } else {
            log.info("YouTube integration enabled");
        }
    }

    /** q puede ser una consulta de texto libre ("Starboy The Weeknd") o un enlace de YouTube —
     * ambos casos devuelven la misma forma de resultado. */
    public List<YoutubeSearchResult> search(UUID actorId, String rawQuery) {
        if (properties.getApiKey().isBlank()) {
            log.warn("Menzi DJ search rejected: YOUTUBE_API_KEY is not configured");
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "YOUTUBE_NOT_CONFIGURED",
                    "La búsqueda de música no está disponible en este momento.");
        }
        String query = normalize(rawQuery);
        if (query.length() < 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SEARCH_QUERY", "Escribí al menos 3 caracteres para buscar.");
        }
        if (query.length() > 100) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SEARCH_QUERY", "La búsqueda es demasiado larga.");
        }

        rateLimiter.checkAndRecord(actorId);

        String linkVideoId = extractVideoId(query);
        String cacheKey = linkVideoId != null ? "id:" + linkVideoId : "q:" + query.toLowerCase();
        List<YoutubeSearchResult> cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<YoutubeSearchResult> results = linkVideoId != null
                ? fetchVideoDetails(List.of(linkVideoId))
                : searchAndEnrich(query);

        cache.put(cacheKey, results);
        return results;
    }

    /** Se usa también para validar un videoId puntual (agregar a cola, aprobar solicitud) — nunca
     * confiamos en los metadatos que mande el cliente, siempre se re-consultan acá. */
    public YoutubeSearchResult getVideo(String videoId) {
        List<YoutubeSearchResult> results = fetchVideoDetails(List.of(videoId));
        if (results.isEmpty()) {
            throw new BadRequestException("Ese video no existe o no está disponible.");
        }
        YoutubeSearchResult result = results.get(0);
        if (!result.embeddable() || result.live()) {
            throw new BadRequestException("Ese video no se puede reproducir en Menzi DJ.");
        }
        return result;
    }

    private List<YoutubeSearchResult> searchAndEnrich(String query) {
        JsonNode searchResponse;
        try {
            searchResponse = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https").host("www.googleapis.com").path("/youtube/v3/search")
                            .queryParam("part", "snippet")
                            .queryParam("type", "video")
                            .queryParam("q", query)
                            .queryParam("videoCategoryId", "10")
                            .queryParam("videoEmbeddable", "true")
                            .queryParam("videoSyndicated", "true")
                            .queryParam("regionCode", properties.getRegionCode())
                            .queryParam("relevanceLanguage", properties.getRelevanceLanguage())
                            .queryParam("maxResults", properties.getMaxResults())
                            .queryParam("key", properties.getApiKey())
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw mapError("search.list", e);
        }

        List<String> videoIds = new ArrayList<>();
        if (searchResponse != null && searchResponse.has("items")) {
            for (JsonNode item : searchResponse.get("items")) {
                String videoId = item.path("id").path("videoId").asText(null);
                if (videoId != null && !videoId.isBlank()) {
                    videoIds.add(videoId);
                }
            }
        }
        if (videoIds.isEmpty()) {
            return List.of();
        }
        return fetchVideoDetails(videoIds);
    }

    /** videos.list trae lo que search.list no da: duración exacta, si es embebible de verdad y si
     * es una transmisión en vivo — search.list ya filtra por videoEmbeddable=true, pero esta
     * segunda consulta es la que realmente confirma esos datos antes de mostrarle algo al usuario. */
    private List<YoutubeSearchResult> fetchVideoDetails(List<String> videoIds) {
        JsonNode response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https").host("www.googleapis.com").path("/youtube/v3/videos")
                            .queryParam("part", "snippet,contentDetails,status,liveStreamingDetails")
                            .queryParam("id", String.join(",", videoIds))
                            .queryParam("key", properties.getApiKey())
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw mapError("videos.list", e);
        }

        List<YoutubeSearchResult> results = new ArrayList<>();
        if (response == null || !response.has("items")) {
            return results;
        }
        for (JsonNode item : response.get("items")) {
            String videoId = item.path("id").asText(null);
            if (videoId == null || videoId.isBlank()) continue;

            JsonNode snippet = item.path("snippet");
            JsonNode status = item.path("status");
            JsonNode contentDetails = item.path("contentDetails");

            boolean embeddable = status.path("embeddable").asBoolean(false);
            String privacyStatus = status.path("privacyStatus").asText("public");
            String liveBroadcastContent = snippet.path("liveBroadcastContent").asText("none");
            boolean isLive = !"none".equals(liveBroadcastContent) || item.has("liveStreamingDetails");

            if (!embeddable || !"public".equals(privacyStatus)) {
                continue; // privado, eliminado o no embebible — se descarta, no se muestra
            }

            Integer durationSeconds = parseIsoDuration(contentDetails.path("duration").asText(null));
            String title = sanitizeTitle(snippet.path("title").asText(""));
            String channelTitle = sanitizeTitle(snippet.path("channelTitle").asText(""));
            String thumbnail = bestThumbnail(snippet.path("thumbnails"));

            results.add(new YoutubeSearchResult(videoId, title, channelTitle, thumbnail, durationSeconds, true, isLive));
        }
        return results;
    }

    private static String bestThumbnail(JsonNode thumbnails) {
        for (String key : new String[] {"high", "medium", "default"}) {
            if (thumbnails.has(key)) {
                return thumbnails.get(key).path("url").asText(null);
            }
        }
        return null;
    }

    /** Los títulos/canales de YouTube pueden traer HTML o caracteres de control — se muestran tal
     * cual (React ya escapa texto por defecto), pero se les quita cualquier etiqueta como defensa
     * en profundidad, igual que TextSanitizer para contenido generado por usuarios de Menzo. */
    private static String sanitizeTitle(String raw) {
        String cleaned = raw.replaceAll("<[^>]*>", "").trim();
        return cleaned.length() > 300 ? cleaned.substring(0, 300) : cleaned;
    }

    private static Integer parseIsoDuration(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return (int) Duration.parse(iso).get(ChronoUnit.SECONDS);
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractVideoId(String query) {
        Matcher matcher = YOUTUBE_LINK.matcher(query);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String normalize(String raw) {
        if (raw == null) return "";
        return raw.trim().replaceAll("\\s+", " ");
    }

    /** Nunca deja pasar la respuesta cruda de Google (puede incluir detalles internos) al cliente —
     * pero sí registra acá (stage, status HTTP, reason de Google, clase de la excepción) para poder
     * diagnosticar sin exponer nunca la API key ni la URL completa (ver sección 2 del pedido). Antes
     * cualquier 403 se asumía "cuota agotada" sin mirar el motivo real — ahora se distingue clave
     * inválida/restringida (reason=keyInvalid/forbidden) de cuota agotada (reason=quotaExceeded/
     * dailyLimitExceeded) leyendo el cuerpo del error de Google. */
    private static ApiException mapError(String stage, RestClientException e) {
        if (e instanceof RestClientResponseException responseException) {
            int status = responseException.getStatusCode().value();
            String reason = extractReason(responseException);
            log.warn("Menzi DJ YouTube call failed: stage={}, status={}, reason={}, errorType={}",
                    stage, status, reason, e.getClass().getSimpleName());

            if (status == 403 && isQuotaReason(reason)) {
                return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "YOUTUBE_QUOTA_EXCEEDED",
                        "Se alcanzó el límite de búsquedas de música por hoy. Intentá más tarde.");
            }
            if (status == 400 || status == 401 || status == 403) {
                return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "YOUTUBE_AUTH_ERROR",
                        "La búsqueda de música no está disponible en este momento.");
            }
            return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "YOUTUBE_UNAVAILABLE",
                    "YouTube no está disponible en este momento.");
        }
        boolean isTimeout = e instanceof ResourceAccessException && e.getCause() instanceof SocketTimeoutException;
        log.warn("Menzi DJ YouTube call failed: stage={}, status=n/a, timeout={}, errorType={}",
                stage, isTimeout, e.getClass().getSimpleName());
        if (isTimeout) {
            return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "YOUTUBE_TIMEOUT",
                    "YouTube tardó demasiado en responder. Intentá de nuevo.");
        }
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "YOUTUBE_UNAVAILABLE", "YouTube no está disponible en este momento.");
    }

    /** Google devuelve el motivo real dentro de error.errors[0].reason — nunca en el status HTTP
     * solo (403 es tanto "cuota agotada" como "clave inválida/restringida"). Se parsea de forma
     * defensiva: si el cuerpo no es el JSON esperado, se sigue igual con reason=null en vez de
     * lanzar una excepción nueva mientras se está manejando una. */
    private static String extractReason(RestClientResponseException responseException) {
        try {
            String body = responseException.getResponseBodyAsString();
            if (body == null || body.isBlank()) return null;
            JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
            JsonNode errors = root.path("error").path("errors");
            if (errors.isArray() && !errors.isEmpty()) {
                return errors.get(0).path("reason").asText(null);
            }
            return null;
        } catch (Exception parseError) {
            return null;
        }
    }

    private static boolean isQuotaReason(String reason) {
        return "quotaExceeded".equals(reason) || "dailyLimitExceeded".equals(reason) || "rateLimitExceeded".equals(reason);
    }
}
