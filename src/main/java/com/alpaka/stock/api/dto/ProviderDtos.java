package com.alpaka.stock.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public final class ProviderDtos {
    private ProviderDtos() {
    }

    public record AlphaDailyIngestRequest(
        @NotBlank String symbol,
        Integer outputSize
    ) {
        public int outputSizeOrDefault() {
            return outputSize == null || outputSize <= 0 ? 100 : Math.min(outputSize, 5000);
        }
    }

    public record AlphaNewsIngestRequest(
        @NotBlank String tickers,
        Integer limit
    ) {
        public int limitOrDefault() {
            return limit == null || limit <= 0 ? 50 : Math.min(limit, 1000);
        }
    }

    public record OpenDartDisclosureIngestRequest(
        @NotBlank String corpCode,
        String symbol,
        LocalDate beginDate,
        LocalDate endDate,
        Integer limit
    ) {
        public LocalDate beginDateOrDefault() {
            return beginDate == null ? LocalDate.now().minusDays(30) : beginDate;
        }

        public LocalDate endDateOrDefault() {
            return endDate == null ? LocalDate.now() : endDate;
        }

        public int limitOrDefault() {
            return limit == null || limit <= 0 ? 50 : Math.min(limit, 100);
        }
    }

    public record SecSubmissionIngestRequest(
        @NotBlank String symbol,
        @NotBlank String cik,
        Integer limit
    ) {
        public int limitOrDefault() {
            return limit == null || limit <= 0 ? 40 : Math.min(limit, 100);
        }
    }

    public record IngestRunResponse(
        String provider,
        String dataType,
        int requested,
        int stored,
        int skipped,
        String message,
        List<String> sourceKeys
    ) {
    }
}
