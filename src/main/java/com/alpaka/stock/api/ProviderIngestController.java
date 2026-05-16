package com.alpaka.stock.api;

import com.alpaka.stock.api.dto.ProviderDtos.AlphaDailyIngestRequest;
import com.alpaka.stock.api.dto.ProviderDtos.AlphaNewsIngestRequest;
import com.alpaka.stock.api.dto.ProviderDtos.IngestRunResponse;
import com.alpaka.stock.api.dto.ProviderDtos.OpenDartDisclosureIngestRequest;
import com.alpaka.stock.api.dto.ProviderDtos.SecSubmissionIngestRequest;
import com.alpaka.stock.service.ProviderIngestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"", "/api/v1"})
@Tag(name = "Provider Ingest", description = "외부 provider 응답을 서버 저장소에 반영하는 API")
public class ProviderIngestController {
    private final ProviderIngestService providerIngestService;

    public ProviderIngestController(ProviderIngestService providerIngestService) {
        this.providerIngestService = providerIngestService;
    }

    @PostMapping("/provider-ingest/alpha-vantage/daily")
    @Operation(summary = "Alpha Vantage 일봉 저장", description = "미국 주식 일봉을 price_bars에 저장합니다.")
    public IngestRunResponse ingestAlphaDaily(@Valid @RequestBody AlphaDailyIngestRequest request) {
        return providerIngestService.ingestAlphaDaily(request);
    }

    @PostMapping("/provider-ingest/alpha-vantage/news")
    @Operation(summary = "Alpha Vantage 뉴스 저장", description = "뉴스 sentiment feed를 source_materials에 저장합니다.")
    public IngestRunResponse ingestAlphaNews(@Valid @RequestBody AlphaNewsIngestRequest request) {
        return providerIngestService.ingestAlphaNews(request);
    }

    @PostMapping("/provider-ingest/opendart/disclosures")
    @Operation(summary = "OpenDART 공시 저장", description = "국내 공시 목록을 source_materials에 저장합니다.")
    public IngestRunResponse ingestOpenDart(@Valid @RequestBody OpenDartDisclosureIngestRequest request) {
        return providerIngestService.ingestOpenDartDisclosures(request);
    }

    @PostMapping("/provider-ingest/sec/submissions")
    @Operation(summary = "SEC 제출 공시 저장", description = "SEC submissions JSON의 최근 filing을 source_materials에 저장합니다.")
    public IngestRunResponse ingestSec(@Valid @RequestBody SecSubmissionIngestRequest request) {
        return providerIngestService.ingestSecSubmissions(request);
    }
}
