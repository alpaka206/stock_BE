package com.alpaka.stock.api.dto;

import com.alpaka.stock.domain.Market;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public final class InstrumentDtos {
    private InstrumentDtos() {
    }

    public record InstrumentUpsertRequest(
        @NotBlank String symbol,
        @NotBlank String name,
        @NotNull Market market,
        @NotBlank String exchange,
        @NotBlank String securityCode,
        String sector,
        @NotBlank String currency,
        boolean active
    ) {
    }

    public record InstrumentResponse(
        UUID id,
        String symbol,
        String name,
        Market market,
        String exchange,
        String securityCode,
        String sector,
        String currency,
        boolean active
    ) {
    }

    public record InstrumentSearchResponse(List<InstrumentResponse> items) {
    }
}
