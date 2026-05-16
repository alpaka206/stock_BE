package com.alpaka.stock.api.dto;

import com.alpaka.stock.domain.ReportCadence;
import com.alpaka.stock.domain.ReportDeliveryStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class ReportDtos {
    private ReportDtos() {
    }

    public record ReportPreviewRequest(
        @NotBlank String userId,
        @Email @NotBlank String deliveryEmail,
        @NotBlank String locale,
        @NotNull ReportCadence cadence,
        List<String> symbols
    ) {
    }

    public record ReportSendRequest(
        @NotBlank String userId,
        @Email @NotBlank String deliveryEmail,
        @NotBlank String locale,
        @NotNull ReportCadence cadence,
        List<String> symbols
    ) {
    }

    public record ReportPreviewResponse(
        String subject,
        String textBody,
        String htmlBody,
        OffsetDateTime generatedAt
    ) {
    }

    public record ReportDeliveryResponse(
        UUID id,
        String userId,
        String deliveryEmail,
        String locale,
        ReportCadence cadence,
        String subject,
        ReportDeliveryStatus status,
        String errorMessage,
        OffsetDateTime generatedAt,
        OffsetDateTime sentAt
    ) {
    }

    public record ReportDeliveryListResponse(List<ReportDeliveryResponse> deliveries) {
    }
}
