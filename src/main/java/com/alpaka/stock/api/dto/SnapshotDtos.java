package com.alpaka.stock.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class SnapshotDtos {
    private SnapshotDtos() {
    }

    public record SnapshotCreateRequest(
        String userId,
        @NotBlank String symbol,
        @NotBlank String note,
        @NotBlank String stance,
        @NotBlank String conviction,
        @NotBlank String thesis,
        @NotNull BigDecimal price,
        @NotNull BigDecimal changePercent,
        @NotNull BigDecimal score,
        String selectedEventTitle,
        String selectedEventDate,
        List<String> activeRuleLabels,
        String presetName
    ) {
    }

    public record SnapshotResponse(
        UUID id,
        String userId,
        String symbol,
        String name,
        String exchange,
        String securityCode,
        String sector,
        String note,
        String stance,
        String conviction,
        BigDecimal price,
        BigDecimal changePercent,
        BigDecimal score,
        String thesis,
        String selectedEventTitle,
        String selectedEventDate,
        List<String> activeRuleLabels,
        String presetName,
        OffsetDateTime createdAt
    ) {
    }

    public record SnapshotListResponse(List<SnapshotResponse> snapshots) {
    }

    public record SnapshotMutationResponse(SnapshotResponse snapshot) {
    }

    public record SnapshotDeleteResponse(UUID id, boolean deleted) {
    }
}
