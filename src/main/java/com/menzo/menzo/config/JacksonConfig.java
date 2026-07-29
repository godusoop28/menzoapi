package com.menzo.menzo.config;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * spring-boot-starter-webmvc (a diferencia del starter "web" clásico) no registra un bean
 * ObjectMapper autoconfigurado por sí solo — se descubrió en producción cuando WebSocketConfig
 * intentó inyectar uno y el arranque falló con "No qualifying bean of type ObjectMapper
 * available". Este es ahora el único ObjectMapper de toda la app: tanto Spring MVC (REST) como
 * WebSocketConfig (STOMP) lo usan, así que ambos serializan fechas de forma idéntica.
 *
 * Instant.now() puede traer precisión de microsegundos o nanosegundos (según el reloj del SO y,
 * acá, la resolución nativa de TIMESTAMPTZ en Postgres), y el serializador JSR-310 por defecto
 * escribe todos esos dígitos fraccionarios sin truncar. Hermes (el motor JS de React Native) no
 * parsea de forma confiable ISO 8601 con más de 3 decimales — Date.parse devuelve Invalid Date,
 * lo que rompía el orden de los mensajes de chat (NaN se trata como "igual" en Array.sort).
 * serializerByType es el mecanismo que Spring expone específicamente para que un serializer
 * propio le gane al de JavaTimeModule para un tipo dado, sin depender del orden en que se
 * registren los módulos — confirmado en vivo que ni modulesToInstall(...) ni un registerModule()
 * posterior lo lograban.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .serializerByType(Instant.class, instantMillisSerializer())
                .build();
    }

    private JsonSerializer<Instant> instantMillisSerializer() {
        return new JsonSerializer<Instant>() {
            @Override
            public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(DateTimeFormatter.ISO_INSTANT.format(value.truncatedTo(ChronoUnit.MILLIS)));
            }
        };
    }
}
