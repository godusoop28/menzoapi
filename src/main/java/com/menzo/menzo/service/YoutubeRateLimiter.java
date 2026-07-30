package com.menzo.menzo.service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.menzo.menzo.exception.TooManyRequestsException;

/** Ventana deslizante en memoria, por usuario — protege la cuota de YouTube Data API de un
 * usuario que busca en bucle. No es distribuido (no sobrevive un restart ni se comparte entre
 * instancias), que es aceptable acá: el límite real de cuota vive en Google, esto solo evita el
 * caso común de alguien mandando la misma tecla repetidamente. */
@Component
public class YoutubeRateLimiter {

    private static final int MAX_REQUESTS = 8;
    private static final long WINDOW_SECONDS = 30;

    private final Map<UUID, Deque<Instant>> requestsByUser = new ConcurrentHashMap<>();

    public void checkAndRecord(UUID userId) {
        Instant cutoff = Instant.now().minusSeconds(WINDOW_SECONDS);
        Deque<Instant> timestamps = requestsByUser.computeIfAbsent(userId, id -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_REQUESTS) {
                throw new TooManyRequestsException("Estás buscando demasiado rápido. Esperá unos segundos e intentá de nuevo.");
            }
            timestamps.addLast(Instant.now());
        }
    }
}
