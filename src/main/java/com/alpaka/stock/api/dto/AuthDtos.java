package com.alpaka.stock.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record DevLoginRequest(
        @Email @NotBlank String email,
        String displayName,
        String locale
    ) {
    }

    public record UserSessionResponse(
        boolean authenticated,
        UUID userId,
        String email,
        String displayName,
        String locale,
        String role,
        OffsetDateTime accessTokenExpiresAt,
        OffsetDateTime refreshTokenExpiresAt
    ) {
    }

    public record LogoutResponse(boolean loggedOut) {
    }
}
