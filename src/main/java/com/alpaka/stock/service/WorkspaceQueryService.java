package com.alpaka.stock.service;

import static com.alpaka.stock.api.dto.CommonDtos.noStoredData;
import static com.alpaka.stock.api.dto.CommonDtos.storedDataConfidence;

import com.alpaka.stock.api.dto.CommonDtos.SourceRefResponse;
import com.alpaka.stock.api.dto.MaterialDtos.MaterialResponse;
import com.alpaka.stock.api.dto.WorkspaceDtos.WorkspaceOverviewResponse;
import com.alpaka.stock.domain.SourceKind;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceQueryService {
    private final MaterialService materialService;

    public WorkspaceQueryService(MaterialService materialService) {
        this.materialService = materialService;
    }

    @Transactional(readOnly = true)
    public WorkspaceOverviewResponse overview() {
        List<MaterialResponse> latestMaterials = materialService.latest(null);
        boolean hasData = !latestMaterials.isEmpty();
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("news", materialService.countByKind(SourceKind.NEWS));
        counts.put("disclosures", materialService.countByKind(SourceKind.DISCLOSURE));
        counts.put("marketData", materialService.countByKind(SourceKind.MARKET_DATA));
        counts.put("earnings", materialService.countByKind(SourceKind.EARNINGS));
        counts.put("fed", materialService.countByKind(SourceKind.FED));
        counts.put("media", materialService.countByKind(SourceKind.MEDIA));

        return new WorkspaceOverviewResponse(
            OffsetDateTime.now(ZoneOffset.UTC),
            latestMaterials.stream().map(this::toSourceRef).toList(),
            hasData ? List.of() : noStoredData("Alpha Vantage, OpenDART, SEC EDGAR, KRX/KIS, Perso"),
            storedDataConfidence(hasData),
            counts,
            latestMaterials
        );
    }

    private SourceRefResponse toSourceRef(MaterialResponse material) {
        return new SourceRefResponse(
            material.id().toString(),
            material.title(),
            material.kind().name().toLowerCase(),
            material.publisher(),
            material.publishedAt(),
            material.sourceUrl(),
            material.sourceKey(),
            material.symbol()
        );
    }
}
