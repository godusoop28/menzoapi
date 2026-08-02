package com.menzo.menzo.security;

import java.util.List;

import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.repository.user.UserRepository;

import io.jsonwebtoken.JwtException;

/**
 * El handshake HTTP del WebSocket no lleva el token — la API {@code WebSocket} nativa (browser y
 * React Native) no permite fijar cabeceras HTTP personalizadas en el upgrade. Por eso la
 * autenticación se hace acá, leyendo el header STOMP "Authorization" que el cliente manda dentro
 * del frame CONNECT (después de que la conexión WS ya está abierta), con la misma lógica de
 * {@link JwtAuthenticationFilter} para el resto de la API.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public StompAuthChannelInterceptor(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new MessagingException("No autenticado");
            }
            try {
                JwtService.DecodedAccessToken decoded = jwtService.parse(header.substring(7));
                User user = userRepository.findById(decoded.userId())
                        .filter(User::isEnabled)
                        .filter(u -> !u.isSuspended())
                        .orElseThrow(() -> new JwtException("Usuario no encontrado"));
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
                accessor.setUser(authentication);
            } catch (JwtException e) {
                throw new MessagingException("No autenticado", e);
            }
        }
        return message;
    }
}
