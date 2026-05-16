package com.alpaka.stock.api.dto;

import com.alpaka.stock.domain.SourceKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class MaterialDtos {
    private MaterialDtos() {
    }

    public record MaterialIngestRequest(
        String symbol,
        @NotNull SourceKind kind,
        @NotBlank String provider,
        @NotBlank String publisher,
        @NotBlank String title,
        String summary,
        String sourceUrl,
        @NotBlank String sourceKey,
        @NotBlank String language,
        OffsetDateTime publishedAt,
        String rawPayload
    ) {
    }

    public record MaterialResponse(
        UUID id,
        String symbol,
        SourceKind kind,
        String provider,
        String publisher,
        String title,
        String summary,
        String sourceUrl,
        String sourceKey,
        String language,
        OffsetDateTime publishedAt
    ) {
    }

    public record MaterialListResponse(List<MaterialResponse> items) {
    }
}
