package com.menzo.menzo.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.menzo.menzo.dto.music.YoutubeSearchResult;

/** Caché en memoria por consulta normalizada — YouTube Data API tiene cuota diaria limitada y la
 * misma búsqueda ("Blinding Lights", por ejemplo) se repite todo el tiempo entre salas distintas.
 * Sin dependencias nuevas (no hay spring-boot-starter-cache en el proyecto): un mapa con TTL
 * manual alcanza para el volumen esperado y es consistente con el resto del código (VoiceService
 * ya usa un ConcurrentHashMap a mano en vez de una librería de caché). */
@Component
public class YoutubeSearchCache {

    private static final long TTL_SECONDS = 12 * 60 * 60; // 12h — "razonable" según el pedido (6-24h)
    private static final int MAX_ENTRIES = 500;

    private record CacheEntry(List<YoutubeSearchResult> results, Instant expiresAt) {
    }

    private final Map<String, CacheEntry> entries = new ConcurrentHashMap<>();

    public List<YoutubeSearchResult> get(String key) {
        CacheEntry entry = entries.get(key);
        if (entry == null) return null;
        if (entry.expiresAt().isBefore(Instant.now())) {
            entries.remove(key);
            return null;
        }
        return entry.results();
    }

    public void put(String key, List<YoutubeSearchResult> results) {
        if (entries.size() >= MAX_ENTRIES) {
            evictOldest();
        }
        entries.put(key, new CacheEntry(results, Instant.now().plusSeconds(TTL_SECONDS)));
    }

    private void evictOldest() {
        entries.entrySet().stream()
                .min(Map.Entry.comparingByValue((a, b) -> a.expiresAt().compareTo(b.expiresAt())))
                .ifPresent(e -> entries.remove(e.getKey()));
    }
}
