package com.menzo.menzo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * `TENOR_API_KEY` es la única variable secreta acá — mismo trato que YoutubeProperties.apiKey:
 * se lee del entorno, nunca se loguea, nunca se serializa en una respuesta ni en un mensaje de
 * excepción, y nunca llega al bundle web ni al APK — los clientes solo le pegan al proxy propio
 * (TenorController), nunca a la API de Tenor directamente.
 */
@ConfigurationProperties(prefix = "tenor")
public class TenorProperties {

    private String apiKey = "";
    private String clientKey = "menzo";
    private int limit = 24;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getClientKey() {
        return clientKey;
    }

    public void setClientKey(String clientKey) {
        this.clientKey = clientKey;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
