package com.menzo.menzo.service;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * `contentDetails.regionRestriction` de videos.list — es distinto de `status.embeddable`: un
 * video puede ser embebible en general y aun así estar bloqueado por el dueño del contenido para
 * países puntuales (licencia musical/regional). YouTube aplica ese bloqueo en el momento real de
 * reproducción, según el país que YouTube le asigna a CADA dispositivo que pide el embed — no
 * según `youtube.region-code` (la config fija de este backend, ver YoutubeProperties). Por eso
 * este chequeo solo puede filtrar, en `search`/`getVideo`, lo que ya sabemos bloqueado para esa
 * región configurada; no puede garantizar que el video vaya a reproducir en el país real de cada
 * usuario — eso solo se confirma con el `onError` real del reproductor (ver
 * menzomovil/lib/features/music/menzi_dj_player_html.dart, YtPlayerError).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record YoutubeRegionRestriction(List<String> allowed, List<String> blocked) {

    /** true si, para `regionCode`, YouTube ya declara este contenido bloqueado — por lista negra
     * explícita, o por una lista blanca que no lo incluye. Sin restricción (`null`) nunca bloquea. */
    boolean blocksRegion(String regionCode) {
        if (regionCode == null) return false;
        if (blocked != null && blocked.contains(regionCode)) return true;
        return allowed != null && !allowed.contains(regionCode);
    }
}
