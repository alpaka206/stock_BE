package com.alpaka.stock.api.dto;

import com.alpaka.stock.api.dto.CommonDtos.ConfidenceResponse;
import com.alpaka.stock.api.dto.CommonDtos.MissingDataResponse;
import com.alpaka.stock.api.dto.CommonDtos.SourceRefResponse;
import com.alpaka.stock.api.dto.MaterialDtos.MaterialResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class WorkspaceDtos {
    private WorkspaceDtos() {
    }

    public record WorkspaceOverviewResponse(
        OffsetDateTime asOf,
        List<SourceRefResponse> sourceRefs,
        List<MissingDataResponse> missingData,
        ConfidenceResponse confidence,
        Map<String, Long> storedCounts,
        List<MaterialResponse> latestMaterials
    ) {
    }
}
