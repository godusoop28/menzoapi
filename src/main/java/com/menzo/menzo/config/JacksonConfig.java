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
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * spring-boot-starter-webmvc (a diferencia del starter "web" clásico) no registra un bean
 * ObjectMapper autoconfigurado por sí solo acá — se descubrió en producción cuando
 * WebSocketConfig intentó inyectar uno y el arranque falló con
 * "No qualifying bean of type ObjectMapper available". Antes de este bean, cada capa
 * (REST, STOMP, etc.) que necesitaba JSON terminaba con su propio ObjectMapper por
 * defecto no compartido — lo cual también explica por qué el módulo de abajo nunca se
 * aplicaba de forma consistente en todos lados. Este bean único, explícito, es ahora la
 * única fuente de verdad para serializar JSON en toda la app.
 *
 * Instant.now() puede traer precisión de nanosegundos (según el reloj del SO), y el
 * serializador JSR-310 por defecto escribe todos esos dígitos fraccionarios sin truncar.
 * Hermes (el motor JS de React Native) no parsea de forma confiable ISO 8601 con más de 3
 * decimales — Date.parse devuelve Invalid Date, lo que rompía el orden de los mensajes de
 * chat (NaN se trata como "igual" en Array.sort). Se trunca a milisegundos acá para que
 * todo timestamp que sale de la API sea válido en cualquier motor JS.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public SimpleModule instantMillisModule() {
        SimpleModule module = new SimpleModule("InstantMillis");
        module.addSerializer(Instant.class, new JsonSerializer<Instant>() {
            @Override
            public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeString(DateTimeFormatter.ISO_INSTANT.format(value.truncatedTo(ChronoUnit.MILLIS)));
            }
        });
        return module;
    }

    /**
     * Jackson2ObjectMapperBuilder ya trae los módulos estándar de Spring Boot (JavaTimeModule,
     * etc.) registrados automáticamente vía findAndRegisterModules(); instantMillisModule() se
     * agrega DESPUÉS con modulesToInstall(...), así que su serializador de Instant gana sobre
     * el de JavaTimeModule para ese mismo tipo. Este es el único ObjectMapper de toda la app —
     * tanto Spring MVC (REST) como el WebSocketConfig (STOMP) lo usan.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .modulesToInstall(instantMillisModule())
                .build();
    }
}
