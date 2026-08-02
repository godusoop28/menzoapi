package com.menzo.menzo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.dto.gif.GifSearchResponse;
import com.menzo.menzo.exception.BadRequestException;
import com.menzo.menzo.service.TenorClient;

/**
 * Proxy propio hacia Tenor — ver TenorClient. `me` no se usa para nada más que exigir que el
 * caller esté autenticado (mismo patrón que el resto de los controllers de esta app): sin esto,
 * este endpoint sería un proxy abierto que cualquiera podría usar para quemar la cuota de la key.
 */
@RestController
@RequestMapping("/api/gifs")
public class TenorController {

    private final TenorClient tenorClient;

    public TenorController(TenorClient tenorClient) {
        this.tenorClient = tenorClient;
    }

    @GetMapping("/search")
    public GifSearchResponse search(
            @RequestParam String q,
            @RequestParam(required = false) String pos,
            @AuthenticationPrincipal User me) {
        if (q == null || q.isBlank()) {
            throw new BadRequestException("Escribí algo para buscar.");
        }
        return tenorClient.search(q, pos);
    }

    @GetMapping("/trending")
    public GifSearchResponse trending(
            @RequestParam(required = false) String pos, @AuthenticationPrincipal User me) {
        return tenorClient.trending(pos);
    }
}
