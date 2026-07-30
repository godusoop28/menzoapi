package com.menzo.menzo.dto.live;

/** role es "PUBLISHER" (HOST/CO_HOST/SPEAKER, puede publicar audio) o "SUBSCRIBER"
 * (AUDIENCE/REQUESTED, solo puede escuchar) — el cliente lo usa para decidir si intenta
 * publicar un track de micrófono o no, pero la autoridad real es el token mismo, generado acá
 * con el rol de Agora correspondiente. */
public record LiveTokenResponse(String appId, String channelName, String token, String uid, String role) {
}
