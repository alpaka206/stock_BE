package com.alpaka.stock.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public final class CommonDtos {
    private CommonDtos() {
    }

    public record ConfidenceResponse(double score, String label, String rationale) {
    }

    public record MissingDataResponse(String field, String reason, String expectedSource) {
    }

    public record SourceRefResponse(
        String id,
        String title,
        String kind,
        String publisher,
        OffsetDateTime publishedAt,
        String url,
        String sourceKey,
        String symbol
    ) {
    }

    public static ConfidenceResponse storedDataConfidence(boolean hasData) {
        if (hasData) {
            return new ConfidenceResponse(0.86, "high", "백엔드에 저장된 원천 자료를 기준으로 응답했습니다.");
        }
        return new ConfidenceResponse(0.2, "low", "아직 저장된 원천 자료가 부족합니다.");
    }

    public static List<MissingDataResponse> noStoredData(String expectedSource) {
        return List.of(new MissingDataResponse(
            "storedMaterials",
            "백엔드에 저장된 원천 자료가 아직 없습니다.",
            expectedSource
        ));
    }
}
