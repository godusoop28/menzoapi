package com.menzo.menzo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.menzo.menzo.exception.TooManyRequestsException;

/** El 429 que aparecía pegado al error de deserialización de Menzi DJ venía de acá (limitador
 * interno por usuario), no de la cuota de Google — ver sección 5 del pedido. */
class YoutubeRateLimiterTest {

    @Test
    void alSuperarElLimite_lanzaConCodigoYRetryAfterPositivo() {
        YoutubeRateLimiter limiter = new YoutubeRateLimiter();
        UUID userId = UUID.randomUUID();

        for (int i = 0; i < 8; i++) {
            limiter.checkAndRecord(userId);
        }

        assertThatThrownBy(() -> limiter.checkAndRecord(userId))
                .isInstanceOf(TooManyRequestsException.class)
                .satisfies(ex -> {
                    TooManyRequestsException tmr = (TooManyRequestsException) ex;
                    assertThat(tmr.getCode()).isEqualTo("MENZI_DJ_RATE_LIMITED");
                    assertThat(tmr.getRetryAfterSeconds()).isNotNull().isPositive();
                });
    }

    @Test
    void usuariosDistintos_noComparenLimite() {
        YoutubeRateLimiter limiter = new YoutubeRateLimiter();
        for (int i = 0; i < 8; i++) {
            limiter.checkAndRecord(UUID.randomUUID());
        }
        // Un usuario nuevo no debería verse afectado por las 8 búsquedas de otros usuarios.
        limiter.checkAndRecord(UUID.randomUUID());
    }
}
