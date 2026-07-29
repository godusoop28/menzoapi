package com.menzo.menzo.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.menzo.menzo.security.StompAuthChannelInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final ObjectMapper objectMapper;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor, ObjectMapper objectMapper) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
        this.objectMapper = objectMapper;
    }

    /**
     * El origen del handshake no es la barrera de seguridad real acá — la app móvil no envía un
     * Origin de navegador que se pueda listar igual que en CorsProperties, y de todas formas quien
     * autentica es {@link StompAuthChannelInterceptor} leyendo el JWT del frame CONNECT (igual que
     * {@code JwtAuthenticationFilter} en el resto de la API). Por eso se permiten todos los orígenes
     * acá en vez de reutilizar la lista de CORS del REST, que es específica de navegadores web.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Bean
    public TaskScheduler webSocketHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic")
                // Sin heartbeat, una conexión medio-caída (celular en segundo plano, NAT que
                // expira) nunca se detecta a nivel STOMP — el cliente cree seguir conectado hasta
                // que algo más falle. 10s/10s deja que @stomp/stompjs dispare su reconnectDelay
                // rápido en vez de quedarse colgado en silencio.
                .setHeartbeatValue(new long[] {10000, 10000})
                .setTaskScheduler(webSocketHeartbeatScheduler());
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Sin esto, los mensajes STOMP se serializan con un ObjectMapper por defecto separado del que
     * usa el resto de la API — que es justo el que tiene registrado el módulo que trunca Instant a
     * milisegundos (ver JacksonConfig). Sin este módulo, un mensaje llega por WebSocket con
     * precisión de nanosegundos, Hermes (y potencialmente otros motores JS) no lo parsea, y el
     * mensaje se ordena mal en el cliente. Compartir el mismo ObjectMapper autoconfigurado de
     * Spring (que ya incluye ese módulo) garantiza que REST y STOMP serialicen fechas idénticas.
     */
    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
        resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        converter.setContentTypeResolver(resolver);
        messageConverters.add(converter);
        return false;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
