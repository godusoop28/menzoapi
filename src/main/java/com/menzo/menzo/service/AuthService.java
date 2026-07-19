package com.menzo.menzo.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.menzo.menzo.domain.auth.RefreshToken;
import com.menzo.menzo.domain.user.User;
import com.menzo.menzo.domain.user.UserSettings;
import com.menzo.menzo.dto.auth.AuthResponse;
import com.menzo.menzo.dto.auth.LoginRequest;
import com.menzo.menzo.dto.auth.RefreshRequest;
import com.menzo.menzo.dto.auth.RegisterRequest;
import com.menzo.menzo.exception.ConflictException;
import com.menzo.menzo.exception.UnauthorizedException;
import com.menzo.menzo.repository.auth.RefreshTokenRepository;
import com.menzo.menzo.repository.user.AuraRepository;
import com.menzo.menzo.repository.user.UserRepository;
import com.menzo.menzo.repository.user.UserSettingsRepository;
import com.menzo.menzo.security.JwtService;
import com.menzo.menzo.security.TokenHasher;
import com.menzo.menzo.service.mapper.ProfileMapper;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String DEFAULT_AURA_ID = "fuego";

    private final UserRepository userRepository;
    private final AuraRepository auraRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ProfileMapper profileMapper;

    public AuthService(
            UserRepository userRepository,
            AuraRepository auraRepository,
            UserSettingsRepository userSettingsRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            ProfileMapper profileMapper) {
        this.userRepository = userRepository;
        this.auraRepository = auraRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.profileMapper = profileMapper;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Ese correo ya está registrado");
        }

        User user = new User();
        user.setEmail(email);
        user.setUsername(generateUniqueUsername());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName("Nuevo usuario");
        user.setAvatarGradient("fire");
        user.setAura(auraRepository.getReferenceById(DEFAULT_AURA_ID));
        user.setJoinedAt(Instant.now());
        user.setOnline(true);
        user.setLastActiveAt(Instant.now());
        user.setOnboardingCompleted(false);
        user = userRepository.save(user);

        userSettingsRepository.save(new UserSettings(user.getId()));

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UnauthorizedException("Credenciales inválidas"));

        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Credenciales inválidas");
        }

        user.setOnline(true);
        user.setLastActiveAt(Instant.now());

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        String hash = TokenHasher.sha256(request.refreshToken());
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Token de actualización inválido"));

        if (!existing.isActive()) {
            throw new UnauthorizedException("Token de actualización expirado o revocado");
        }

        User user = userRepository.findById(existing.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));

        existing.setRevokedAt(Instant.now());

        IssuedTokens issued = issueTokens(user);
        existing.setReplacedById(issued.refreshTokenEntity().getId());

        return toAuthResponse(user, issued);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        String hash = TokenHasher.sha256(request.refreshToken());
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
            }
        });
    }

    private AuthResponse buildAuthResponse(User user) {
        return toAuthResponse(user, issueTokens(user));
    }

    private AuthResponse toAuthResponse(User user, IssuedTokens issued) {
        return new AuthResponse(
                issued.accessToken(),
                issued.rawRefreshToken(),
                jwtService.accessTokenExpiry(),
                user.getEmail(),
                user.isOnboardingCompleted(),
                profileMapper.toProfile(user, user.getId()));
    }

    private IssuedTokens issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);

        String rawRefreshToken = TokenHasher.newOpaqueToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(user.getId());
        refreshToken.setTokenHash(TokenHasher.sha256(rawRefreshToken));
        refreshToken.setExpiresAt(Instant.now().plus(jwtService.refreshTokenTtl()));
        refreshToken = refreshTokenRepository.save(refreshToken);

        return new IssuedTokens(accessToken, rawRefreshToken, refreshToken);
    }

    private record IssuedTokens(String accessToken, String rawRefreshToken, RefreshToken refreshTokenEntity) {
    }

    private String generateUniqueUsername() {
        String candidate;
        do {
            candidate = "user" + (100000 + RANDOM.nextInt(900000));
        } while (userRepository.existsByUsernameIgnoreCase(candidate));
        return candidate;
    }
}
