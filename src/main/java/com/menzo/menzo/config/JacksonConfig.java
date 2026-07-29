package com.menzo.menzo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * spring-boot-starter-webmvc (a diferencia del starter "web" clásico) no registra un bean
 * ObjectMapper autoconfigurado por sí solo — se descubrió en producción cuando WebSocketConfig
 * intentó inyectar uno y el arranque falló con "No qualifying bean of type ObjectMapper
 * available". Este es ahora el único ObjectMapper de toda la app: tanto Spring MVC (REST) como
 * WebSocketConfig (STOMP) lo usan, así que ambos serializan igual.
 *
 * Nota sobre fechas: se probaron tres formas distintas de forzar que java.time.Instant se
 * serialice truncado a milisegundos acá (modulesToInstall, registerModule posterior,
 * serializerByType) — ninguna le ganó al serializer de JavaTimeModule en este entorno
 * (Spring Boot 4.1 / Spring 7), confirmado en vivo contra el backend desplegado. En vez de seguir
 * peleando con el orden de resolución de serializers de Jackson, los DTOs que de verdad importan
 * para el orden de mensajes en tiempo real (ver MessageResponse) formatean la fecha ellos mismos
 * como String antes de llegar acá, así el resultado no depende de esta configuración global.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json().build();
    }
}
