package com.menzo.menzo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Cubre la parte de YoutubeClient que rompía en producción: el mapeo de la respuesta JSON de
 * YouTube Data API a YoutubeSearchResult. No levanta un servidor HTTP de mentira — search()/
 * fetchVideoDetails() llaman a googleapis.com con una URL fija y este proyecto no tiene un
 * RestClient.Builder inyectable para redirigirlas (spring-boot-starter-restclient no es una
 * dependencia acá). En cambio, se ejercen directamente los métodos package-private de mapeo
 * (extractVideoIds, toSearchResult, bestThumbnail, parseIsoDuration, extractVideoId, extractReason)
 * con JSON simulado deserializado por un ObjectMapper de Jackson 3 — que es exactamente donde
 * ocurría el InvalidDefinitionException real (deserializar la respuesta de Google), no en el
 * transporte HTTP en sí.
 */
class YoutubeClientMappingTest {

    // Mismo tipo de ObjectMapper (Jackson 3, tools.jackson.databind) que usa Spring para poblar
    // los DTOs de RestClient.body(...) — así el test reproduce el paso real que fallaba en
    // producción, no una simulación aproximada.
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    // ---- 1. search.list válido ----------------------------------------------------------------

    @Test
    void searchListValido_extraeLosVideoIds() {
        String json = """
                {
                  "kind": "youtube#searchListResponse",
                  "etag": "abc",
                  "regionCode": "AR",
                  "pageInfo": {"totalResults": 2, "resultsPerPage": 2},
                  "items": [
                    {
                      "kind": "youtube#searchResult",
                      "id": {"kind": "youtube#video", "videoId": "dQw4w9WgXcQ"},
                      "snippet": {"title": "irrelevante acá"}
                    },
                    {
                      "kind": "youtube#searchResult",
                      "id": {"kind": "youtube#video", "videoId": "9bZkp7q19f0"}
                    }
                  ]
                }
                """;
        YoutubeSearchResponse response = MAPPER.readValue(json, YoutubeSearchResponse.class);
        List<String> videoIds = YoutubeClient.extractVideoIds(response);
        assertThat(videoIds).containsExactly("dQw4w9WgXcQ", "9bZkp7q19f0");
    }

    @Test
    void searchListConIdOVideoIdAusente_seIgnoraEseItem() {
        String json = """
                {"items": [
                  {"id": {"kind": "youtube#video"}},
                  {"id": {"kind": "youtube#video", "videoId": "9bZkp7q19f0"}},
                  {}
                ]}
                """;
        YoutubeSearchResponse response = MAPPER.readValue(json, YoutubeSearchResponse.class);
        assertThat(YoutubeClient.extractVideoIds(response)).containsExactly("9bZkp7q19f0");
    }

    // ---- 2. videos.list válido -----------------------------------------------------------------

    @Test
    void videosListValido_mapeaAYoutubeSearchResult() {
        String json = """
                {
                  "kind": "youtube#videoListResponse",
                  "items": [{
                    "kind": "youtube#video",
                    "id": "dQw4w9WgXcQ",
                    "snippet": {
                      "title": "Rick Astley - Never Gonna Give You Up",
                      "channelTitle": "Rick Astley",
                      "liveBroadcastContent": "none",
                      "thumbnails": {
                        "default": {"url": "https://i.ytimg.com/vi/dQw4w9WgXcQ/default.jpg"},
                        "medium": {"url": "https://i.ytimg.com/vi/dQw4w9WgXcQ/mqdefault.jpg"},
                        "high": {"url": "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg"}
                      }
                    },
                    "contentDetails": {"duration": "PT3M33S"},
                    "status": {"embeddable": true, "privacyStatus": "public"}
                  }]
                }
                """;
        YoutubeVideosResponse response = MAPPER.readValue(json, YoutubeVideosResponse.class);
        assertThat(response.items()).hasSize(1);

        var result = YoutubeClient.toSearchResult(response.items().get(0), "MX");
        assertThat(result).isNotNull();
        assertThat(result.videoId()).isEqualTo("dQw4w9WgXcQ");
        assertThat(result.title()).isEqualTo("Rick Astley - Never Gonna Give You Up");
        assertThat(result.channelTitle()).isEqualTo("Rick Astley");
        assertThat(result.thumbnailUrl()).isEqualTo("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg");
        assertThat(result.durationSeconds()).isEqualTo(213);
        assertThat(result.embeddable()).isTrue();
        assertThat(result.live()).isFalse();
    }

    @Test
    void videoNoEmbebibleOPrivado_seDescarta() {
        String noEmbeddable = """
                {"id": "abc", "status": {"embeddable": false, "privacyStatus": "public"}}
                """;
        String privado = """
                {"id": "abc", "status": {"embeddable": true, "privacyStatus": "private"}}
                """;
        assertThat(YoutubeClient.toSearchResult(MAPPER.readValue(noEmbeddable, YoutubeVideoItem.class), "MX")).isNull();
        assertThat(YoutubeClient.toSearchResult(MAPPER.readValue(privado, YoutubeVideoItem.class), "MX")).isNull();
    }

    // ---- 2b. contentDetails.regionRestriction ---------------------------------------------------

    @Test
    void videoBloqueadoPorListaNegraEnLaRegionConfigurada_seDescarta() throws IOException {
        String json = """
                {"id": "abc", "status": {"embeddable": true, "privacyStatus": "public"},
                 "contentDetails": {"regionRestriction": {"blocked": ["MX", "AR"]}}}
                """;
        assertThat(YoutubeClient.toSearchResult(MAPPER.readValue(json, YoutubeVideoItem.class), "MX")).isNull();
    }

    @Test
    void videoConListaBlancaSinLaRegionConfigurada_seDescarta() throws IOException {
        String json = """
                {"id": "abc", "status": {"embeddable": true, "privacyStatus": "public"},
                 "contentDetails": {"regionRestriction": {"allowed": ["US", "CA"]}}}
                """;
        assertThat(YoutubeClient.toSearchResult(MAPPER.readValue(json, YoutubeVideoItem.class), "MX")).isNull();
    }

    @Test
    void videoConListaBlancaQueIncluyeLaRegionConfigurada_noSeDescarta() throws IOException {
        String json = """
                {"id": "abc", "status": {"embeddable": true, "privacyStatus": "public"},
                 "contentDetails": {"regionRestriction": {"allowed": ["US", "MX"]}}}
                """;
        assertThat(YoutubeClient.toSearchResult(MAPPER.readValue(json, YoutubeVideoItem.class), "MX")).isNotNull();
    }

    @Test
    void sinRegionRestriction_noSeDescartaPorRegion() throws IOException {
        String json = """
                {"id": "abc", "status": {"embeddable": true, "privacyStatus": "public"},
                 "contentDetails": {"duration": "PT1M"}}
                """;
        assertThat(YoutubeClient.toSearchResult(MAPPER.readValue(json, YoutubeVideoItem.class), "MX")).isNotNull();
    }

    @Test
    void videoEnVivo_seMarcaLiveConLiveBroadcastContentOLiveStreamingDetails() {
        String porLiveBroadcastContent = """
                {"id": "abc", "status": {"embeddable": true, "privacyStatus": "public"},
                 "snippet": {"liveBroadcastContent": "live"}}
                """;
        String porLiveStreamingDetails = """
                {"id": "abc", "status": {"embeddable": true, "privacyStatus": "public"},
                 "liveStreamingDetails": {"actualStartTime": "2026-01-01T00:00:00Z"}}
                """;
        assertThat(YoutubeClient.toSearchResult(MAPPER.readValue(porLiveBroadcastContent, YoutubeVideoItem.class), "MX").live()).isTrue();
        assertThat(YoutubeClient.toSearchResult(MAPPER.readValue(porLiveStreamingDetails, YoutubeVideoItem.class), "MX").live()).isTrue();
    }

    // ---- 3. URL directa de YouTube --------------------------------------------------------------

    @Test
    void extraeVideoIdDeDistintosFormatosDeUrl() {
        assertThat(YoutubeClient.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ")).isEqualTo("dQw4w9WgXcQ");
        assertThat(YoutubeClient.extractVideoId("https://youtu.be/dQw4w9WgXcQ")).isEqualTo("dQw4w9WgXcQ");
        assertThat(YoutubeClient.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ")).isEqualTo("dQw4w9WgXcQ");
        assertThat(YoutubeClient.extractVideoId("https://www.youtube.com/embed/dQw4w9WgXcQ")).isEqualTo("dQw4w9WgXcQ");
    }

    @Test
    void textoLibreSinUrl_noExtraeVideoId() {
        assertThat(YoutubeClient.extractVideoId("mago de oz")).isNull();
        assertThat(YoutubeClient.extractVideoId("Starboy The Weeknd")).isNull();
    }

    // ---- 4. duración PT4M13S / inválida ---------------------------------------------------------

    @Test
    void parseaDuracionIso8601() {
        assertThat(YoutubeClient.parseIsoDuration("PT4M13S")).isEqualTo(253);
        assertThat(YoutubeClient.parseIsoDuration("PT1H2M3S")).isEqualTo(3723);
    }

    @Test
    void duracionInvalidaOAusente_devuelveNullSinLanzar() {
        assertThat(YoutubeClient.parseIsoDuration(null)).isNull();
        assertThat(YoutubeClient.parseIsoDuration("")).isNull();
        assertThat(YoutubeClient.parseIsoDuration("no-es-iso-8601")).isNull();
    }

    // ---- 5. items vacío / nulo -------------------------------------------------------------------

    @Test
    void itemsVacio_devuelveListaVacia() throws IOException {
        YoutubeSearchResponse response = MAPPER.readValue("{\"items\": []}", YoutubeSearchResponse.class);
        assertThat(YoutubeClient.extractVideoIds(response)).isEmpty();
    }

    @Test
    void itemsAusente_noLanzaYDevuelveListaVacia() throws IOException {
        YoutubeSearchResponse response = MAPPER.readValue("{\"kind\": \"youtube#searchListResponse\"}", YoutubeSearchResponse.class);
        assertThat(response.items()).isNull();
        assertThat(YoutubeClient.extractVideoIds(response)).isEmpty();
        assertThat(YoutubeClient.extractVideoIds(null)).isEmpty();
    }

    // ---- 6. thumbnail ausente ----------------------------------------------------------------------

    @Test
    void thumbnailAusente_bestThumbnailDevuelveNullSinLanzar() throws IOException {
        YoutubeSnippet sinThumbnails = MAPPER.readValue("{\"title\": \"t\"}", YoutubeSnippet.class);
        assertThat(sinThumbnails.thumbnails()).isNull();
        assertThat(YoutubeClient.bestThumbnail(sinThumbnails.thumbnails())).isNull();

        String json = "{\"id\": \"abc\", \"status\": {\"embeddable\": true, \"privacyStatus\": \"public\"}, "
                + "\"snippet\": {\"title\": \"sin miniatura\"}}";
        var result = YoutubeClient.toSearchResult(MAPPER.readValue(json, YoutubeVideoItem.class), "MX");
        assertThat(result).isNotNull();
        assertThat(result.thumbnailUrl()).isNull();
    }

    // ---- 7. contentDetails ausente -------------------------------------------------------------

    @Test
    void contentDetailsAusente_duracionQuedaNullSinLanzar() throws IOException {
        String json = "{\"id\": \"abc\", \"status\": {\"embeddable\": true, \"privacyStatus\": \"public\"}}";
        var result = YoutubeClient.toSearchResult(MAPPER.readValue(json, YoutubeVideoItem.class), "MX");
        assertThat(result).isNotNull();
        assertThat(result.durationSeconds()).isNull();
    }

    @Test
    void statusOSnippetAusente_noLanzaYSeManejaComoNoEmbebible() throws IOException {
        String sinStatus = "{\"id\": \"abc\"}";
        assertThat(YoutubeClient.toSearchResult(MAPPER.readValue(sinStatus, YoutubeVideoItem.class), "MX")).isNull();
    }

    // ---- 8. respuesta de error de Google (quotaExceeded) ----------------------------------------

    @Test
    void extraeReasonDelCuerpoDeErrorDeGoogle() {
        String errorBody = """
                {
                  "error": {
                    "code": 403,
                    "message": "The request cannot be completed because you have exceeded your quota.",
                    "errors": [
                      {"domain": "youtube.quota", "reason": "quotaExceeded", "message": "Quota exceeded"}
                    ]
                  }
                }
                """;
        RestClientResponseException exception = HttpClientErrorException.create(
                HttpStatus.FORBIDDEN, "Forbidden", HttpHeaders.EMPTY,
                errorBody.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        assertThat(YoutubeClient.extractReason(exception)).isEqualTo("quotaExceeded");
    }

    @Test
    void cuerpoDeErrorInesperado_extractReasonDevuelveNullSinLanzar() {
        RestClientResponseException exception = HttpClientErrorException.create(
                HttpStatus.FORBIDDEN, "Forbidden", HttpHeaders.EMPTY,
                "<html>esto no es JSON</html>".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        assertThat(YoutubeClient.extractReason(exception)).isNull();
    }

    // ---- 9. JSON válido procesado con Jackson 3 --------------------------------------------------

    @Test
    void seDeserializaConObjectMapperDeJackson3() {
        // tools.jackson.databind.ObjectMapper, no com.fasterxml.jackson.databind.ObjectMapper —
        // si esta línea compila e importa "tools.jackson.databind.json.JsonMapper" es la prueba de
        // que YoutubeClient ya no depende del ObjectMapper de Jackson 2 para nada.
        ObjectMapper jackson3Mapper = JsonMapper.builder().build();
        YoutubeVideosResponse response = jackson3Mapper.readValue(
                "{\"items\": [{\"id\": \"x\", \"status\": {\"embeddable\": true, \"privacyStatus\": \"public\"}}]}",
                YoutubeVideosResponse.class);
        assertThat(response.items()).hasSize(1);
    }

    // ---- 10. confirmar ausencia de com.fasterxml.jackson.databind.JsonNode en YoutubeClient ------

    @Test
    void youtubeClientNoImportaJsonNodeDeJackson2() throws IOException {
        Path source = Path.of("src", "main", "java", "com", "menzo", "menzo", "service", "YoutubeClient.java");
        List<String> importLines = Files.readAllLines(source, StandardCharsets.UTF_8).stream()
                .filter(line -> line.trim().startsWith("import "))
                .toList();
        // Ningún import debe venir de com.fasterxml.jackson.databind (Jackson 2) — solo se permite
        // com.fasterxml.jackson.annotation (compartido entre Jackson 2 y 3, ver YoutubeStatus/
        // YoutubeThumbnails) y tools.jackson.databind (Jackson 3, ver ERROR_BODY_MAPPER).
        assertThat(importLines).noneMatch(line -> line.contains("com.fasterxml.jackson.databind"));
        assertThat(importLines).noneMatch(line -> line.contains("com.fasterxml.jackson.core"));
        assertThat(importLines).anyMatch(line -> line.contains("tools.jackson.databind"));
    }
}
