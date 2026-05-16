package com.alpaka.stock.api.dto;

import com.alpaka.stock.domain.LocalizationStatus;
import com.alpaka.stock.domain.MediaKind;
import com.alpaka.stock.domain.ReportCadence;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PlatformDtos {
    private PlatformDtos() {
    }

    public record SubscriptionPlanResponse(
        UUID id,
        String code,
        String name,
        BigDecimal monthlyPrice,
        String currency,
        String featureLimits,
        boolean active
    ) {
    }

    public record SubscriptionPlanListResponse(List<SubscriptionPlanResponse> plans) {
    }

    public record ReportScheduleRequest(
        @NotBlank String userId,
        @NotBlank String locale,
        @NotNull ReportCadence cadence,
        @Email @NotBlank String deliveryEmail,
        @NotBlank String timezone,
        Boolean enabled
    ) {
        public boolean enabledOrDefault() {
            return enabled == null || enabled;
        }
    }

    public record ReportScheduleResponse(
        UUID id,
        String userId,
        String locale,
        ReportCadence cadence,
        String deliveryEmail,
        String timezone,
        boolean enabled,
        OffsetDateTime createdAt
    ) {
    }

    public record ReportScheduleListResponse(List<ReportScheduleResponse> schedules) {
    }

    public record MediaAssetRequest(
        String symbol,
        UUID materialId,
        @NotNull MediaKind kind,
        @NotBlank String title,
        @NotBlank String sourceUrl,
        @NotBlank String provider,
        @NotBlank String language,
        OffsetDateTime publishedAt
    ) {
    }

    public record MediaAssetResponse(
        UUID id,
        String symbol,
        UUID materialId,
        MediaKind kind,
        String title,
        String sourceUrl,
        String provider,
        String language,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt
    ) {
    }

    public record MediaAssetListResponse(List<MediaAssetResponse> assets) {
    }

    public record LocalizationJobRequest(
        @NotNull UUID mediaAssetId,
        @NotBlank String provider,
        @NotBlank String targetLanguage
    ) {
    }

    public record LocalizationJobResponse(
        UUID id,
        UUID mediaAssetId,
        String provider,
        String targetLanguage,
        LocalizationStatus status,
        String dubbedAudioUrl,
        String subtitleUrl,
        String errorMessage,
        OffsetDateTime requestedAt,
        OffsetDateTime completedAt
    ) {
    }

    public record LocalizationJobListResponse(List<LocalizationJobResponse> jobs) {
    }

    public record AutomationWebhookRequest(
        @NotBlank String eventType,
        Map<String, Object> payload
    ) {
    }

    public record AutomationWebhookResponse(
        UUID id,
        String source,
        String eventType,
        String status,
        OffsetDateTime receivedAt
    ) {
    }
}
