package com.menzo.menzo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * `YOUTUBE_API_KEY` es la única variable secreta acá — se lee del entorno, nunca se loguea, nunca
 * se serializa en una respuesta ni en un mensaje de excepción (ver YoutubeClient: cualquier error
 * de la API de Google se traduce a un mensaje genérico antes de llegar al cliente).
 */
@ConfigurationProperties(prefix = "youtube")
public class YoutubeProperties {

    private String apiKey = "";
    private String regionCode = "MX";
    private String relevanceLanguage = "es";
    private int maxResults = 10;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public String getRelevanceLanguage() {
        return relevanceLanguage;
    }

    public void setRelevanceLanguage(String relevanceLanguage) {
        this.relevanceLanguage = relevanceLanguage;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }
}
