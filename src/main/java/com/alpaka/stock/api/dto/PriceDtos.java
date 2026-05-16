package com.alpaka.stock.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class PriceDtos {
    private PriceDtos() {
    }

    public record PriceBarUpsertRequest(
        @NotBlank String symbol,
        @NotNull LocalDate tradeDate,
        @NotNull BigDecimal openPrice,
        @NotNull BigDecimal highPrice,
        @NotNull BigDecimal lowPrice,
        @NotNull BigDecimal closePrice,
        @NotNull BigDecimal volume,
        @NotBlank String provider,
        @NotBlank String sourceKey
    ) {
    }

    public record PriceBarResponse(
        UUID id,
        String symbol,
        LocalDate tradeDate,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        BigDecimal volume,
        String provider,
        String sourceKey
    ) {
    }

    public record PriceBarListResponse(List<PriceBarResponse> items) {
    }
}
