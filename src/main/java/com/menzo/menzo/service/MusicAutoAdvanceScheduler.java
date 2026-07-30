package com.menzo.menzo.service;

import java.util.UUID;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** El backend es la autoridad de la reproducción (ver sección 10 del pedido) — nadie confía en
 * que algún cliente avise "esta canción terminó", porque no siempre hay uno abierto de forma
 * confiable para todos. Este scheduler revisa cada 5s si alguna sesión PLAYING ya alcanzó la
 * duración de su video actual y la avanza sola a la siguiente de la cola. */
@Component
public class MusicAutoAdvanceScheduler {

    private final MusicService musicService;

    public MusicAutoAdvanceScheduler(MusicService musicService) {
        this.musicService = musicService;
    }

    @Scheduled(fixedRate = 5_000)
    public void advanceFinishedTracks() {
        for (UUID musicSessionId : musicService.findSessionsNeedingAutoAdvance()) {
            musicService.autoAdvance(musicSessionId);
        }
    }
}
