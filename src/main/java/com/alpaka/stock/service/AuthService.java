package com.alpaka.stock.service;

import com.alpaka.stock.api.dto.AuthDtos.UserSessionResponse;
import com.alpaka.stock.domain.RefreshSession;
import com.alpaka.stock.domain.UserAccount;
import com.alpaka.stock.repository.RefreshSessionRepository;
import com.alpaka.stock.repository.UserAccountRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final UserAccountRepository userAccountRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String accessCookieName;
    private final String refreshCookieName;
    private final String cookieDomain;
    private final boolean cookieSecure;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final String jwtIssuer;
    private final byte[] jwtSecret;

    public AuthService(
        UserAccountRepository userAccountRepository,
        RefreshSessionRepository refreshSessionRepository,
        ObjectMapper objectMapper,
        @Value("${stock.auth.access-cookie-name}") String accessCookieName,
        @Value("${stock.auth.refresh-cookie-name}") String refreshCookieName,
        @Value("${stock.auth.cookie-domain}") String cookieDomain,
        @Value("${stock.auth.cookie-secure}") boolean cookieSecure,
        @Value("${stock.auth.access-token-ttl-minutes}") long accessTokenTtlMinutes,
        @Value("${stock.auth.refresh-token-ttl-days}") long refreshTokenTtlDays,
        @Value("${stock.auth.jwt-issuer}") String jwtIssuer,
        @Value("${stock.auth.jwt-secret}") String jwtSecret
    ) {
        this.userAccountRepository = userAccountRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.objectMapper = objectMapper;
        this.accessCookieName = accessCookieName;
        this.refreshCookieName = refreshCookieName;
        this.cookieDomain = cookieDomain;
        this.cookieSecure = cookieSecure;
        this.accessTokenTtl = Duration.ofMinutes(accessTokenTtlMinutes);
        this.refreshTokenTtl = Duration.ofDays(refreshTokenTtlDays);
        this.jwtIssuer = jwtIssuer;
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("AUTH_JWT_SECRET must be at least 32 characters.");
        }
        this.jwtSecret = jwtSecret.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public UserSessionResponse login(
        String provider,
        String providerUserId,
        String email,
        String displayName,
        String locale,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        UserAccount user = userAccountRepository
            .findByProviderIgnoreCaseAndProviderUserId(provider, providerUserId)
            .or(() -> userAccountRepository.findByEmailIgnoreCase(email))
            .orElseGet(() -> new UserAccount(
                provider,
                providerUserId,
                email,
                displayName,
                normalizeLocale(locale),
                "USER"
            ));
        user.updateProfile(email, displayName, normalizeLocale(locale));
        UserAccount saved = userAccountRepository.save(user);
        return issueSession(saved, request, response);
    }

    @Transactional
    public UserSessionResponse refresh(String refreshToken, HttpServletRequest request, HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("refresh token이 없습니다.");
        }
        RefreshSession session = refreshSessionRepository.findByTokenHash(hashToken(refreshToken))
            .filter(item -> item.isUsable(now()))
            .orElseThrow(() -> new IllegalArgumentException("refresh token이 유효하지 않습니다."));
        session.revoke();
        return issueSession(session.getUser(), request, response);
    }

    @Transactional
    public void logout(String refreshToken, HttpServletResponse response) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshSessionRepository.findByTokenHash(hashToken(refreshToken)).ifPresent(RefreshSession::revoke);
        }
        clearCookie(response, accessCookieName);
        clearCookie(response, refreshCookieName);
    }

    @Transactional(readOnly = true)
    public UserSessionResponse currentUser(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return anonymous();
        }
        try {
            Map<String, Object> payload = verifyAccessToken(accessToken);
            UUID userId = UUID.fromString(String.valueOf(payload.get("sub")));
            return userAccountRepository.findById(userId)
                .filter(UserAccount::isActive)
                .map(user -> toResponse(
                    user,
                    OffsetDateTime.ofInstant(
                        java.time.Instant.ofEpochSecond(numberClaim(payload.get("exp"))),
                        ZoneOffset.UTC
                    ),
                    null
                ))
                .orElseGet(this::anonymous);
        } catch (RuntimeException exc) {
            return anonymous();
        }
    }

    public String accessCookieName() {
        return accessCookieName;
    }

    public String refreshCookieName() {
        return refreshCookieName;
    }

    private UserSessionResponse issueSession(
        UserAccount user,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        OffsetDateTime accessExpiresAt = now().plus(accessTokenTtl);
        OffsetDateTime refreshExpiresAt = now().plus(refreshTokenTtl);
        String accessToken = createAccessToken(user, accessExpiresAt);
        String refreshToken = createOpaqueToken();
        RefreshSession refreshSession = new RefreshSession(
            user,
            hashToken(refreshToken),
            request.getHeader("User-Agent"),
            request.getRemoteAddr(),
            refreshExpiresAt
        );
        refreshSessionRepository.save(refreshSession);
        addCookie(response, accessCookieName, accessToken, accessTokenTtl);
        addCookie(response, refreshCookieName, refreshToken, refreshTokenTtl);
        return toResponse(user, accessExpiresAt, refreshExpiresAt);
    }

    private String createAccessToken(UserAccount user, OffsetDateTime expiresAt) {
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("iss", jwtIssuer);
        payload.put("sub", user.getId().toString());
        payload.put("email", user.getEmail());
        payload.put("role", user.getRole());
        payload.put("iat", now().toEpochSecond());
        payload.put("exp", expiresAt.toEpochSecond());
        String unsigned = base64Json(header) + "." + base64Json(payload);
        return unsigned + "." + sign(unsigned);
    }

    private Map<String, Object> verifyAccessToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("JWT 형식이 올바르지 않습니다.");
        }
        String unsigned = parts[0] + "." + parts[1];
        if (!MessageDigest.isEqual(sign(unsigned).getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalArgumentException("JWT 서명이 올바르지 않습니다.");
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), MAP_TYPE);
            if (!jwtIssuer.equals(payload.get("iss"))) {
                throw new IllegalArgumentException("JWT issuer가 올바르지 않습니다.");
            }
            if (numberClaim(payload.get("exp")) <= now().toEpochSecond()) {
                throw new IllegalArgumentException("JWT가 만료됐습니다.");
            }
            return payload;
        } catch (Exception exc) {
            throw new IllegalArgumentException("JWT를 해석할 수 없습니다.", exc);
        }
    }

    private String base64Json(Map<String, Object> value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exc) {
            throw new IllegalStateException("토큰 payload를 만들 수 없습니다.", exc);
        }
    }

    private String sign(String unsigned) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtSecret, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(unsigned.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exc) {
            throw new IllegalStateException("access token 서명에 실패했습니다.", exc);
        }
    }

    private String createOpaqueToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        return HexFormat.of().formatHex(sha256(token));
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exc) {
            throw new IllegalStateException("refresh token hash에 실패했습니다.", exc);
        }
    }

    private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("Lax")
            .path("/")
            .maxAge(maxAge);
        if (!cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private void clearCookie(HttpServletResponse response, String name) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ZERO);
        if (!cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private UserSessionResponse toResponse(
        UserAccount user,
        OffsetDateTime accessExpiresAt,
        OffsetDateTime refreshExpiresAt
    ) {
        return new UserSessionResponse(
            true,
            user.getId(),
            user.getEmail(),
            user.getDisplayName(),
            user.getLocale(),
            user.getRole(),
            accessExpiresAt,
            refreshExpiresAt
        );
    }

    private UserSessionResponse anonymous() {
        return new UserSessionResponse(false, null, null, null, null, null, null, null);
    }

    private String normalizeLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return "ko";
        }
        return locale.trim().toLowerCase();
    }

    private long numberClaim(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
