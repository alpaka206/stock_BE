package com.alpaka.stock.service;

import com.alpaka.stock.api.dto.CommonDtos.ConfidenceResponse;
import com.alpaka.stock.api.dto.CommonDtos.MissingDataResponse;
import com.alpaka.stock.api.dto.CommonDtos.SourceRefResponse;
import com.alpaka.stock.domain.Instrument;
import com.alpaka.stock.domain.PriceBar;
import com.alpaka.stock.domain.SourceKind;
import com.alpaka.stock.domain.SourceMaterial;
import com.alpaka.stock.repository.InstrumentRepository;
import com.alpaka.stock.repository.PriceBarRepository;
import com.alpaka.stock.repository.SourceMaterialRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContractViewService {
    private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    private final InstrumentRepository instrumentRepository;
    private final PriceBarRepository priceBarRepository;
    private final SourceMaterialRepository sourceMaterialRepository;

    public ContractViewService(
        InstrumentRepository instrumentRepository,
        PriceBarRepository priceBarRepository,
        SourceMaterialRepository sourceMaterialRepository
    ) {
        this.instrumentRepository = instrumentRepository;
        this.priceBarRepository = priceBarRepository;
        this.sourceMaterialRepository = sourceMaterialRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> overview() {
        List<SourceMaterial> materials = sourceMaterialRepository.findTop50ByOrderByPublishedAtDesc();
        List<InstrumentPrice> instrumentPrices = latestInstrumentPrices();
        List<SourceRefResponse> sourceRefs = mergeSourceRefs(
            materials.stream().limit(20).map(material -> toSourceRef(material, false)).toList(),
            instrumentPrices.stream()
                .map(InstrumentPrice::latest)
                .map(this::toSourceRef)
                .limit(8)
                .toList()
        );
        List<String> sourceIds = sourceIds(sourceRefs);
        boolean hasData = !materials.isEmpty() || !instrumentPrices.isEmpty();

        return object(
            "asOf", OffsetDateTime.now(ZoneOffset.UTC),
            "sourceRefs", sourceRefs,
            "missingData", missingData(
                hasData,
                "overview",
                "Alpha Vantage, KIS/KRX, OpenDART, SEC EDGAR, FRED"
            ),
            "confidence", confidence(hasData),
            "benchmarkSnapshot", benchmarkSnapshot(instrumentPrices),
            "marketSummary", textBlock(
                firstSummary(materials, "저장된 시장 자료가 아직 없습니다. 데이터 공급자를 연결하면 국내장과 미국장 흐름을 한 화면에서 볼 수 있습니다."),
                sourceIds
            ),
            "drivers", materials.stream()
                .filter(this::isDriverCandidate)
                .limit(3)
                .map(material -> textBlock(summaryOrTitle(material), List.of(material.getId().toString())))
                .toList(),
            "risks", materials.stream()
                .filter(this::isRiskCandidate)
                .limit(3)
                .map(material -> textBlock(summaryOrTitle(material), List.of(material.getId().toString())))
                .toList(),
            "sectorStrength", sectorStrength(instrumentPrices),
            "notableNews", notableNews(materials)
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> radar() {
        List<Instrument> instruments = instrumentRepository.findTop100ByActiveTrueOrderBySymbolAsc();
        List<InstrumentPrice> rows = instruments.stream()
            .map(this::toInstrumentPrice)
            .filter(InstrumentPrice::hasPrice)
            .toList();
        List<SourceMaterial> materials = sourceMaterialRepository.findTop50ByOrderByPublishedAtDesc();
        List<SourceRefResponse> sourceRefs = mergeSourceRefs(
            materials.stream().limit(20).map(material -> toSourceRef(material, false)).toList(),
            rows.stream().map(InstrumentPrice::latest).map(this::toSourceRef).limit(20).toList()
        );
        List<String> sourceIds = sourceIds(sourceRefs);
        boolean hasData = !rows.isEmpty() || !materials.isEmpty();

        return object(
            "asOf", OffsetDateTime.now(ZoneOffset.UTC),
            "sourceRefs", sourceRefs,
            "missingData", missingData(hasData, "watchlistRows", "저장된 종목, 가격, 뉴스/공시 자료"),
            "confidence", confidence(hasData),
            "selectedSectorSummary", textBlock(
                hasData
                    ? "저장된 종목과 원천 자료를 기준으로 관심 종목의 가격 변화, 이슈, 이벤트를 정리했습니다."
                    : "저장된 관심 종목 데이터가 아직 없습니다.",
                sourceIds
            ),
            "folderTree", folderTree(instruments),
            "watchlistRows", rows.stream().map(this::watchlistRow).toList(),
            "sectorCards", sectorCards(rows),
            "brokerReports", brokerReports(materials),
            "keySchedule", keySchedule(materials),
            "keyIssues", keyIssues(materials),
            "topPicks", topPicks(rows),
            "alertRules", alertRules(),
            "detectedAlerts", detectedAlerts(rows)
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> stock(String symbol) {
        Instrument instrument = instrumentRepository.findFirstBySymbolIgnoreCase(symbol)
            .orElseThrow(() -> new EntityNotFoundException("종목을 찾을 수 없습니다: " + symbol));
        List<PriceBar> bars = ascendingBars(instrument);
        List<SourceMaterial> materials = sourceMaterialRepository.findTop20ByInstrumentOrderByPublishedAtDesc(instrument);
        List<SourceRefResponse> sourceRefs = mergeSourceRefs(
            latestBar(bars).stream().map(this::toSourceRef).toList(),
            materials.stream().map(material -> toSourceRef(material, false)).toList()
        );
        List<String> sourceIds = sourceIds(sourceRefs);
        double latestPrice = latestBar(bars).map(bar -> toDouble(bar.getClosePrice())).orElse(0.0);
        double changePercent = changePercentFromAscending(bars);
        double score = scoreFromChange(changePercent, bars.size());

        return object(
            "asOf", OffsetDateTime.now(ZoneOffset.UTC),
            "sourceRefs", sourceRefs,
            "missingData", missingData(
                !bars.isEmpty() || !materials.isEmpty(),
                "stockDetail",
                "가격 데이터, 뉴스, 공시, 실적 자료"
            ),
            "confidence", confidence(!bars.isEmpty() || !materials.isEmpty()),
            "instrument", object(
                "symbol", instrument.getSymbol(),
                "name", instrument.getName(),
                "exchange", instrument.getExchange(),
                "securityCode", instrument.getSecurityCode(),
                "sector", nullToEmpty(instrument.getSector()),
                "marketCap", "미수집"
            ),
            "latestPrice", latestPrice,
            "changePercent", changePercent,
            "thesis", firstSummary(materials, "저장된 뉴스와 공시가 부족합니다. 가격 데이터와 직접 수집한 자료를 추가하면 판단 근거가 강화됩니다."),
            "priceSeries", priceSeries(bars),
            "eventMarkers", eventMarkers(materials, bars),
            "indicatorGuides", indicatorGuides(bars),
            "chartOverlays", chartOverlays(bars),
            "technicalMetrics", technicalMetrics(bars, sourceIds),
            "patternCards", patternCards(bars, sourceIds),
            "rulePresetDefinitions", rulePresetDefinitions(),
            "scoreSummary", scoreSummary(score, bars.size(), materials.size()),
            "flowMetrics", List.of(),
            "flowUnavailable", unavailable("수급 데이터", "증권사/거래소 수급 API가 아직 연결되지 않았습니다.", "KIS, KRX, 증권사별 수급 API"),
            "optionsShortMetrics", List.of(),
            "optionsUnavailable", unavailable("옵션/공매도 데이터", "옵션과 공매도 데이터 공급자가 아직 연결되지 않았습니다.", "OPRA, KRX, FINRA"),
            "issueCards", issueCards(materials),
            "relatedSymbols", relatedSymbols(instrument)
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> history(String symbol, String range) {
        Instrument instrument = resolveHistoryInstrument(symbol);
        List<PriceBar> bars = instrument == null ? List.of() : ascendingBars(instrument);
        List<SourceMaterial> materials = instrument == null
            ? sourceMaterialRepository.findTop20ByOrderByPublishedAtDesc()
            : sourceMaterialRepository.findTop20ByInstrumentOrderByPublishedAtDesc(instrument);
        List<SourceRefResponse> sourceRefs = mergeSourceRefs(
            latestBar(bars).stream().map(this::toSourceRef).toList(),
            materials.stream().map(material -> toSourceRef(material, false)).toList()
        );
        List<String> sourceIds = sourceIds(sourceRefs);
        List<Map<String, Object>> timeline = eventTimeline(materials, bars, instrument);

        return object(
            "asOf", OffsetDateTime.now(ZoneOffset.UTC),
            "sourceRefs", sourceRefs,
            "missingData", missingData(
                !bars.isEmpty() || !timeline.isEmpty(),
                "history",
                "가격 기록, 뉴스, 공시, 실적 이벤트"
            ),
            "confidence", confidence(!bars.isEmpty() || !timeline.isEmpty()),
            "symbol", instrument == null ? nullToEmpty(symbol) : instrument.getSymbol(),
            "rangeLabel", rangeLabel(range),
            "availableRanges", availableRanges(),
            "priceSeries", priceSeries(bars),
            "eventMarkers", eventMarkers(materials, bars),
            "eventTimeline", timeline,
            "moveSummary", textBlock(moveSummary(bars), sourceIds),
            "moveReasons", moveReasons(bars, sourceIds),
            "overlappingIndicators", overlappingIndicators(bars, sourceIds),
            "analogsOrPatterns", analogsOrPatterns(bars, sourceIds)
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> news() {
        List<SourceMaterial> materials = sourceMaterialRepository.findTop50ByOrderByPublishedAtDesc();
        List<SourceRefResponse> sourceRefs = materials.stream()
            .limit(30)
            .map(material -> toSourceRef(material, true))
            .toList();
        List<String> sourceIds = sourceIds(sourceRefs);
        List<Map<String, Object>> items = materials.stream().map(this::newsItem).toList();

        return object(
            "asOf", OffsetDateTime.now(ZoneOffset.UTC),
            "sourceRefs", sourceRefs,
            "missingData", missingData(!materials.isEmpty(), "news", "뉴스, 공시, 실적, 경제 이벤트 공급자"),
            "confidence", confidence(!materials.isEmpty()),
            "marketSummary", textBlock(firstSummary(materials, "저장된 뉴스와 공시가 아직 없습니다."), sourceIds),
            "newsDrivers", materials.stream()
                .limit(3)
                .map(material -> textBlock(summaryOrTitle(material), List.of(material.getId().toString())))
                .toList(),
            "featuredNews", items.stream().limit(8).toList(),
            "watchlistNews", items.stream().filter(item -> !nullToEmpty((String) item.get("symbol")).isBlank()).limit(12).toList(),
            "domesticDisclosures", items.stream().filter(item -> "domestic".equals(item.get("market"))).limit(12).toList()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> calendar() {
        List<SourceMaterial> materials = sourceMaterialRepository.findTop50ByOrderByPublishedAtDesc();
        List<SourceRefResponse> sourceRefs = materials.stream()
            .limit(30)
            .map(material -> toSourceRef(material, true))
            .toList();
        List<String> sourceIds = sourceIds(sourceRefs);
        List<Map<String, Object>> events = materials.stream()
            .filter(this::isCalendarCandidate)
            .map(this::calendarEvent)
            .toList();

        return object(
            "asOf", OffsetDateTime.now(ZoneOffset.UTC),
            "sourceRefs", sourceRefs,
            "missingData", missingData(!events.isEmpty(), "calendar", "실적 발표, FOMC, 공시, IPO 일정"),
            "confidence", confidence(!events.isEmpty()),
            "calendarSummary", textBlock(firstSummary(materials, "저장된 일정 자료가 아직 없습니다."), sourceIds),
            "highlights", calendarHighlights(events, sourceIds),
            "watchlistEvents", events.stream().filter(event -> "watchlist".equals(event.get("market"))).limit(12).toList(),
            "marketEvents", events.stream().filter(event -> "global".equals(event.get("market"))).limit(12).toList(),
            "domesticEvents", events.stream().filter(event -> "domestic".equals(event.get("market"))).limit(12).toList()
        );
    }

    private List<InstrumentPrice> latestInstrumentPrices() {
        return instrumentRepository.findTop100ByActiveTrueOrderBySymbolAsc()
            .stream()
            .map(this::toInstrumentPrice)
            .filter(InstrumentPrice::hasPrice)
            .toList();
    }

    private InstrumentPrice toInstrumentPrice(Instrument instrument) {
        List<PriceBar> latestBars = priceBarRepository.findTop2ByInstrumentOrderByTradeDateDesc(instrument);
        PriceBar latest = latestBars.isEmpty() ? null : latestBars.get(0);
        PriceBar previous = latestBars.size() > 1 ? latestBars.get(1) : null;
        return new InstrumentPrice(instrument, latest, previous);
    }

    private List<Map<String, Object>> benchmarkSnapshot(List<InstrumentPrice> instrumentPrices) {
        return instrumentPrices.stream()
            .limit(6)
            .map(item -> object(
                "label", item.instrument().getName(),
                "symbol", item.instrument().getSymbol(),
                "category", item.instrument().getMarket().name(),
                "value", toDouble(item.latest().getClosePrice()),
                "changePercent", item.changePercent(),
                "note", "백엔드 저장 가격 기준",
                "sourceRefIds", List.of(item.latest().getId().toString())
            ))
            .toList();
    }

    private List<Map<String, Object>> sectorStrength(List<InstrumentPrice> instrumentPrices) {
        Map<String, List<InstrumentPrice>> bySector = new LinkedHashMap<>();
        for (InstrumentPrice item : instrumentPrices) {
            bySector.computeIfAbsent(normalizedSector(item.instrument()), key -> new ArrayList<>()).add(item);
        }

        return bySector.entrySet().stream()
            .map(entry -> {
                double averageChange = entry.getValue().stream().mapToDouble(InstrumentPrice::changePercent).average().orElse(0);
                double score = scoreFromChange(averageChange, entry.getValue().size());
                return object(
                    "sector", entry.getKey(),
                    "score", round(score),
                    "summary", "저장된 가격 기준 평균 등락률 " + round(averageChange) + "%",
                    "changePercent", round(averageChange),
                    "sourceRefIds", entry.getValue().stream().map(item -> item.latest().getId().toString()).limit(5).toList()
                );
            })
            .sorted(Comparator.comparing(item -> -((Number) item.get("score")).doubleValue()))
            .limit(6)
            .toList();
    }

    private List<Map<String, Object>> notableNews(List<SourceMaterial> materials) {
        return materials.stream()
            .filter(material -> material.getKind() == SourceKind.NEWS || material.getKind() == SourceKind.DISCLOSURE)
            .limit(6)
            .map(material -> object(
                "headline", material.getTitle(),
                "source", material.getPublisher(),
                "summary", summaryOrTitle(material),
                "impact", impactFromKind(material.getKind()),
                "publishedAt", publishedAt(material),
                "url", nullToEmpty(material.getSourceUrl()),
                "sourceRefIds", List.of(material.getId().toString())
            ))
            .toList();
    }

    private List<Map<String, Object>> folderTree(List<Instrument> instruments) {
        Map<String, List<Instrument>> bySector = new LinkedHashMap<>();
        for (Instrument instrument : instruments) {
            bySector.computeIfAbsent(normalizedSector(instrument), key -> new ArrayList<>()).add(instrument);
        }

        return bySector.entrySet().stream()
            .map(entry -> object(
                "id", slug(entry.getKey()),
                "label", entry.getKey(),
                "count", entry.getValue().size(),
                "description", "저장된 관심 종목 " + entry.getValue().size() + "개",
                "tags", entry.getValue().stream().map(item -> item.getMarket().name()).distinct().toList(),
                "children", List.of()
            ))
            .toList();
    }

    private Map<String, Object> watchlistRow(InstrumentPrice item) {
        double changePercent = item.changePercent();
        double score = scoreFromChange(changePercent, 1);
        return object(
            "symbol", item.instrument().getSymbol(),
            "name", item.instrument().getName(),
            "securityCode", item.instrument().getSecurityCode(),
            "sector", normalizedSector(item.instrument()),
            "folderId", slug(normalizedSector(item.instrument())),
            "tags", List.of(item.instrument().getMarket().name(), item.instrument().getExchange()),
            "price", toDouble(item.latest().getClosePrice()),
            "changePercent", round(changePercent),
            "volumeRatio", 1.0,
            "relativeStrength", round(Math.max(0, Math.min(100, 50 + changePercent * 5))),
            "score", round(score),
            "nextEvent", "저장된 이벤트 확인 필요",
            "thesis", "저장된 가격과 원천 자료 기준으로 추적 중입니다.",
            "condition", changePercent >= 0 ? "가격 모멘텀 확인" : "하락 리스크 확인",
            "sourceRefIds", List.of(item.latest().getId().toString())
        );
    }

    private List<Map<String, Object>> sectorCards(List<InstrumentPrice> rows) {
        return sectorStrength(rows).stream()
            .map(sector -> object(
                "sector", sector.get("sector"),
                "score", sector.get("score"),
                "thesis", sector.get("summary"),
                "catalyst", "가격 변화와 저장 뉴스 동시 확인",
                "topPick", rows.stream()
                    .filter(row -> normalizedSector(row.instrument()).equals(sector.get("sector")))
                    .max(Comparator.comparingDouble(row -> scoreFromChange(row.changePercent(), 1)))
                    .map(row -> row.instrument().getSymbol())
                    .orElse(""),
                "sourceRefIds", sector.get("sourceRefIds")
            ))
            .toList();
    }

    private List<Map<String, Object>> brokerReports(List<SourceMaterial> materials) {
        return materials.stream()
            .filter(material -> material.getKind() == SourceKind.FUNDAMENTALS || material.getKind() == SourceKind.EARNINGS)
            .limit(8)
            .map(material -> object(
                "sector", material.getInstrument() == null ? "시장" : normalizedSector(material.getInstrument()),
                "house", material.getPublisher(),
                "symbol", material.getInstrument() == null ? "" : material.getInstrument().getSymbol(),
                "stance", "확인 필요",
                "summary", summaryOrTitle(material),
                "sourceRefIds", List.of(material.getId().toString())
            ))
            .toList();
    }

    private List<Map<String, Object>> keySchedule(List<SourceMaterial> materials) {
        return materials.stream()
            .filter(this::isCalendarCandidate)
            .limit(8)
            .map(material -> object(
                "sector", material.getInstrument() == null ? "시장" : normalizedSector(material.getInstrument()),
                "time", publishedAt(material).toString(),
                "title", material.getTitle(),
                "note", summaryOrTitle(material),
                "sourceRefIds", List.of(material.getId().toString())
            ))
            .toList();
    }

    private List<Map<String, Object>> keyIssues(List<SourceMaterial> materials) {
        return materials.stream()
            .limit(8)
            .map(material -> object(
                "headline", material.getTitle(),
                "summary", summaryOrTitle(material),
                "impact", impactFromKind(material.getKind()),
                "sector", material.getInstrument() == null ? "시장" : normalizedSector(material.getInstrument()),
                "sourceRefIds", List.of(material.getId().toString())
            ))
            .toList();
    }

    private List<Map<String, Object>> topPicks(List<InstrumentPrice> rows) {
        return rows.stream()
            .sorted(Comparator.comparingDouble((InstrumentPrice item) -> scoreFromChange(item.changePercent(), 1)).reversed())
            .limit(5)
            .map(item -> object(
                "sector", normalizedSector(item.instrument()),
                "symbol", item.instrument().getSymbol(),
                "reason", "저장 가격 기준 상대 모멘텀 상위",
                "score", round(scoreFromChange(item.changePercent(), 1)),
                "sourceRefIds", List.of(item.latest().getId().toString())
            ))
            .toList();
    }

    private List<Map<String, Object>> alertRules() {
        return List.of(
            object("id", "price-move", "label", "가격 급변", "description", "최근 저장 가격 변화가 5% 이상이면 표시", "severity", "watch", "enabledByDefault", true),
            object("id", "data-gap", "label", "자료 공백", "description", "가격 또는 뉴스 원천 자료가 부족하면 표시", "severity", "info", "enabledByDefault", true),
            object("id", "risk-check", "label", "하락 점검", "description", "최근 가격 변화가 -5% 이하이면 표시", "severity", "critical", "enabledByDefault", true)
        );
    }

    private List<Map<String, Object>> detectedAlerts(List<InstrumentPrice> rows) {
        return rows.stream()
            .filter(item -> Math.abs(item.changePercent()) >= 5)
            .limit(10)
            .map(item -> object(
                "id", "alert-" + item.instrument().getSymbol(),
                "ruleId", item.changePercent() < 0 ? "risk-check" : "price-move",
                "symbol", item.instrument().getSymbol(),
                "title", item.changePercent() < 0 ? "하락 리스크 점검" : "가격 모멘텀 확인",
                "summary", "최근 저장 가격 기준 등락률 " + round(item.changePercent()) + "%",
                "severity", item.changePercent() < 0 ? "critical" : "watch",
                "tone", item.changePercent() < 0 ? "negative" : "positive",
                "triggeredAt", OffsetDateTime.now(ZoneOffset.UTC),
                "sourceRefIds", List.of(item.latest().getId().toString())
            ))
            .toList();
    }

    private List<Map<String, Object>> priceSeries(List<PriceBar> bars) {
        return bars.stream()
            .map(bar -> object(
                "date", bar.getTradeDate().toString(),
                "label", bar.getTradeDate().format(DAY_LABEL_FORMATTER),
                "close", toDouble(bar.getClosePrice()),
                "volume", toDouble(bar.getVolume())
            ))
            .toList();
    }

    private List<Map<String, Object>> eventMarkers(List<SourceMaterial> materials, List<PriceBar> bars) {
        List<Map<String, Object>> markers = new ArrayList<>();
        materials.stream().limit(6).forEach(material -> markers.add(object(
            "id", material.getId().toString(),
            "label", labelFromKind(material.getKind()),
            "tone", toneFromKind(material.getKind()),
            "date", publishedAt(material).toLocalDate().toString(),
            "pointLabel", labelFromKind(material.getKind()),
            "title", material.getTitle(),
            "detail", summaryOrTitle(material),
            "href", nullToEmpty(material.getSourceUrl())
        )));

        if (markers.isEmpty()) {
            latestBar(bars).ifPresent(bar -> markers.add(object(
                "id", bar.getId().toString(),
                "label", "가격",
                "tone", "neutral",
                "date", bar.getTradeDate().toString(),
                "pointLabel", "종가",
                "title", "최근 저장 가격",
                "detail", "저장된 일별 가격 데이터 기준",
                "href", ""
            )));
        }
        return markers;
    }

    private List<Map<String, Object>> indicatorGuides(List<PriceBar> bars) {
        List<Map<String, Object>> guides = new ArrayList<>();
        addMovingAverageGuide(guides, bars, 5);
        addMovingAverageGuide(guides, bars, 20);
        addMovingAverageGuide(guides, bars, 60);
        return guides;
    }

    private void addMovingAverageGuide(List<Map<String, Object>> guides, List<PriceBar> bars, int window) {
        OptionalDouble average = movingAverage(bars, window);
        if (average.isEmpty()) {
            return;
        }
        double latest = latestBar(bars).map(bar -> toDouble(bar.getClosePrice())).orElse(0.0);
        guides.add(object(
            "id", "ma" + window,
            "label", window + "일선",
            "value", round(average.getAsDouble()),
            "tone", latest >= average.getAsDouble() ? "positive" : "negative",
            "description", "저장된 종가 기준 이동평균",
            "enabled", window <= 20
        ));
    }

    private List<Map<String, Object>> chartOverlays(List<PriceBar> bars) {
        List<Map<String, Object>> overlays = new ArrayList<>();
        addMovingAverageOverlay(overlays, bars, 5);
        addMovingAverageOverlay(overlays, bars, 20);
        addMovingAverageOverlay(overlays, bars, 60);
        return overlays;
    }

    private void addMovingAverageOverlay(List<Map<String, Object>> overlays, List<PriceBar> bars, int window) {
        if (bars.size() < window) {
            return;
        }
        List<Map<String, Object>> points = new ArrayList<>();
        for (int index = window - 1; index < bars.size(); index++) {
            double average = bars.subList(index - window + 1, index + 1)
                .stream()
                .mapToDouble(bar -> toDouble(bar.getClosePrice()))
                .average()
                .orElse(0);
            PriceBar bar = bars.get(index);
            points.add(object(
                "label", bar.getTradeDate().format(DAY_LABEL_FORMATTER),
                "value", round(average),
                "date", bar.getTradeDate().toString()
            ));
        }
        overlays.add(object(
            "id", "ma" + window,
            "label", window + "일 이동평균",
            "tone", window <= 20 ? "positive" : "neutral",
            "points", points,
            "enabled", window <= 20
        ));
    }

    private List<Map<String, Object>> technicalMetrics(List<PriceBar> bars, List<String> sourceIds) {
        if (bars.isEmpty()) {
            return List.of();
        }
        double change = changePercentFromAscending(bars);
        List<Map<String, Object>> metrics = new ArrayList<>();
        metrics.add(object(
            "id", "change",
            "label", "최근 등락률",
            "value", round(change) + "%",
            "detail", "저장된 최근 두 가격 기록 기준",
            "tone", change >= 0 ? "positive" : "negative",
            "sourceRefIds", sourceIds
        ));
        movingAverage(bars, 20).ifPresent(value -> metrics.add(object(
            "id", "ma20",
            "label", "20일선",
            "value", String.valueOf(round(value)),
            "detail", "저장 종가 기준 단기 추세",
            "tone", latestBar(bars).map(bar -> toDouble(bar.getClosePrice())).orElse(0.0) >= value ? "positive" : "negative",
            "sourceRefIds", sourceIds
        )));
        metrics.add(object(
            "id", "volume",
            "label", "최근 거래량",
            "value", latestBar(bars).map(bar -> String.valueOf(bar.getVolume().setScale(0, RoundingMode.HALF_UP))).orElse("0"),
            "detail", "저장된 마지막 거래일 기준",
            "tone", "neutral",
            "sourceRefIds", sourceIds
        ));
        return metrics;
    }

    private List<Map<String, Object>> patternCards(List<PriceBar> bars, List<String> sourceIds) {
        if (bars.size() < 20) {
            return List.of();
        }
        double change = changePercentFromAscending(bars);
        return List.of(object(
            "id", "stored-trend",
            "label", "저장 가격 추세",
            "similarity", Math.min(1.0, Math.max(0.2, Math.abs(change) / 10)),
            "stage", change >= 0 ? "상승 확인" : "하락 점검",
            "invalidation", "20일 이동평균 이탈 여부 확인",
            "summary", "최근 저장 가격과 이동평균을 함께 비교한 단순 추세 점검입니다.",
            "tone", change >= 0 ? "positive" : "negative",
            "sourceRefIds", sourceIds
        ));
    }

    private List<Map<String, Object>> rulePresetDefinitions() {
        return List.of(
            object("id", "price", "label", "가격", "description", "최근 가격과 이동평균을 확인", "enabledByDefault", true, "tone", "neutral", "guideIds", List.of("ma5", "ma20"), "controlsEventMarkers", false),
            object("id", "events", "label", "이벤트", "description", "뉴스, 공시, 실적 이벤트 표시", "enabledByDefault", true, "tone", "neutral", "guideIds", List.of(), "controlsEventMarkers", true),
            object("id", "risk", "label", "리스크", "description", "하락 변동성과 자료 공백을 점검", "enabledByDefault", true, "tone", "negative", "guideIds", List.of("ma20"), "controlsEventMarkers", false)
        );
    }

    private Map<String, Object> scoreSummary(double total, int priceCount, int materialCount) {
        ConfidenceResponse confidence = confidence(priceCount > 0 || materialCount > 0);
        return object(
            "total", round(total),
            "confidence", confidence,
            "breakdown", List.of(
                object("label", "가격 기록", "score", Math.min(40, priceCount), "summary", "저장 가격 " + priceCount + "개"),
                object("label", "원천 자료", "score", Math.min(40, materialCount * 4), "summary", "뉴스/공시/이벤트 " + materialCount + "개"),
                object("label", "모멘텀", "score", round(Math.max(0, Math.min(20, total - 40))), "summary", "최근 등락률 기반")
            )
        );
    }

    private Map<String, Object> unavailable(String label, String reason, String expectedSource) {
        return object("label", label, "reason", reason, "expectedSource", expectedSource);
    }

    private List<Map<String, Object>> issueCards(List<SourceMaterial> materials) {
        return materials.stream()
            .limit(8)
            .map(material -> object(
                "title", material.getTitle(),
                "source", material.getPublisher(),
                "summary", summaryOrTitle(material),
                "tone", toneFromKind(material.getKind()),
                "category", labelFromKind(material.getKind()),
                "href", nullToEmpty(material.getSourceUrl()),
                "sourceRefIds", List.of(material.getId().toString())
            ))
            .toList();
    }

    private List<String> relatedSymbols(Instrument instrument) {
        if (instrument.getSector() == null || instrument.getSector().isBlank()) {
            return List.of();
        }
        return instrumentRepository.findTop12BySectorIgnoreCaseAndActiveTrueOrderBySymbolAsc(instrument.getSector())
            .stream()
            .map(Instrument::getSymbol)
            .filter(symbol -> !symbol.equalsIgnoreCase(instrument.getSymbol()))
            .limit(6)
            .toList();
    }

    private List<Map<String, Object>> eventTimeline(List<SourceMaterial> materials, List<PriceBar> bars, Instrument instrument) {
        List<Map<String, Object>> timeline = materials.stream()
            .limit(12)
            .map(material -> object(
                "id", material.getId().toString(),
                "date", publishedAt(material).toLocalDate().toString(),
                "title", material.getTitle(),
                "category", labelFromKind(material.getKind()),
                "summary", summaryOrTitle(material),
                "reaction", "원천 자료 확인 필요",
                "tone", toneFromKind(material.getKind()),
                "source", material.getPublisher(),
                "url", nullToEmpty(material.getSourceUrl()),
                "sourceRefIds", List.of(material.getId().toString())
            ))
            .toList();

        if (!timeline.isEmpty() || bars.isEmpty()) {
            return timeline;
        }

        PriceBar latest = latestBar(bars).orElseThrow();
        return List.of(object(
            "id", latest.getId().toString(),
            "date", latest.getTradeDate().toString(),
            "title", (instrument == null ? "" : instrument.getSymbol()) + " 최근 저장 가격",
            "category", "가격",
            "summary", "저장된 일별 가격 데이터 기준 종가 " + latest.getClosePrice(),
            "reaction", "가격 변화와 원천 뉴스의 연결 확인 필요",
            "tone", "neutral",
            "source", latest.getProvider(),
            "url", "",
            "sourceRefIds", List.of(latest.getId().toString())
        ));
    }

    private String moveSummary(List<PriceBar> bars) {
        if (bars.isEmpty()) {
            return "저장된 가격 기록이 아직 없습니다.";
        }
        return "최근 저장 가격 기준 등락률은 " + round(changePercentFromAscending(bars)) + "%입니다.";
    }

    private List<Map<String, Object>> moveReasons(List<PriceBar> bars, List<String> sourceIds) {
        if (bars.size() < 2) {
            return List.of();
        }
        double change = changePercentFromAscending(bars);
        return List.of(object(
            "label", change >= 0 ? "상승 요인 점검" : "하락 요인 점검",
            "description", "가격 변화는 확인됐지만 뉴스/공시와의 직접 연결은 원천 자료 추가 후 판단합니다.",
            "tone", change >= 0 ? "positive" : "negative",
            "relatedDate", latestBar(bars).map(bar -> bar.getTradeDate().toString()).orElse(""),
            "sourceRefIds", sourceIds
        ));
    }

    private List<Map<String, Object>> overlappingIndicators(List<PriceBar> bars, List<String> sourceIds) {
        List<Map<String, Object>> indicators = new ArrayList<>();
        movingAverage(bars, 20).ifPresent(value -> indicators.add(object(
            "label", "20일선 비교",
            "detail", "최근 종가와 20일 이동평균을 비교했습니다.",
            "tone", latestBar(bars).map(bar -> toDouble(bar.getClosePrice())).orElse(0.0) >= value ? "positive" : "negative",
            "relatedDate", latestBar(bars).map(bar -> bar.getTradeDate().toString()).orElse(""),
            "sourceRefIds", sourceIds
        )));
        return indicators;
    }

    private List<Map<String, Object>> analogsOrPatterns(List<PriceBar> bars, List<String> sourceIds) {
        if (bars.size() < 20) {
            return List.of();
        }
        return List.of(textBlock("20거래일 이상 저장된 가격으로 단기 추세 패턴을 비교할 수 있습니다.", sourceIds));
    }

    private Map<String, Object> newsItem(SourceMaterial material) {
        return object(
            "id", material.getId().toString(),
            "headline", material.getTitle(),
            "source", material.getPublisher(),
            "summary", summaryOrTitle(material),
            "impact", impactFromKind(material.getKind()),
            "publishedAt", publishedAt(material),
            "url", nullToEmpty(material.getSourceUrl()),
            "symbol", material.getInstrument() == null ? "" : material.getInstrument().getSymbol(),
            "market", marketGroup(material),
            "sourceRefIds", List.of(material.getId().toString())
        );
    }

    private Map<String, Object> calendarEvent(SourceMaterial material) {
        return object(
            "id", material.getId().toString(),
            "title", material.getTitle(),
            "category", calendarCategory(material.getKind()),
            "market", marketGroup(material),
            "date", publishedAt(material).toLocalDate().toString(),
            "time", publishedAt(material).toOffsetTime().toString(),
            "summary", summaryOrTitle(material),
            "source", material.getPublisher(),
            "symbol", material.getInstrument() == null ? "" : material.getInstrument().getSymbol(),
            "url", nullToEmpty(material.getSourceUrl()),
            "tone", toneFromKind(material.getKind()),
            "sourceRefIds", List.of(material.getId().toString())
        );
    }

    private List<Map<String, Object>> calendarHighlights(List<Map<String, Object>> events, List<String> sourceIds) {
        if (events.isEmpty()) {
            return List.of();
        }
        return List.of(object(
            "label", "저장 일정",
            "value", events.size() + "건",
            "detail", "뉴스, 공시, 실적, 경제 자료에서 추출한 일정성 이벤트",
            "tone", "neutral",
            "sourceRefIds", sourceIds
        ));
    }

    private Instrument resolveHistoryInstrument(String symbol) {
        if (symbol != null && !symbol.isBlank()) {
            return instrumentRepository.findFirstBySymbolIgnoreCase(symbol).orElse(null);
        }
        return instrumentRepository.findTop100ByActiveTrueOrderBySymbolAsc()
            .stream()
            .filter(instrument -> priceBarRepository.countByInstrument(instrument) > 0)
            .findFirst()
            .orElse(null);
    }

    private List<PriceBar> ascendingBars(Instrument instrument) {
        List<PriceBar> bars = new ArrayList<>(priceBarRepository.findTop180ByInstrumentOrderByTradeDateDesc(instrument));
        bars.sort(Comparator.comparing(PriceBar::getTradeDate));
        return bars;
    }

    private OptionalDouble movingAverage(List<PriceBar> bars, int window) {
        if (bars.size() < window) {
            return OptionalDouble.empty();
        }
        return bars.subList(bars.size() - window, bars.size())
            .stream()
            .mapToDouble(bar -> toDouble(bar.getClosePrice()))
            .average();
    }

    private java.util.Optional<PriceBar> latestBar(List<PriceBar> bars) {
        if (bars.isEmpty()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(bars.get(bars.size() - 1));
    }

    private SourceRefResponse toSourceRef(SourceMaterial material, boolean allowDisclosureKind) {
        return new SourceRefResponse(
            material.getId().toString(),
            material.getTitle(),
            sourceKind(material.getKind(), allowDisclosureKind),
            material.getPublisher(),
            publishedAt(material),
            nullToEmpty(material.getSourceUrl()),
            material.getSourceKey(),
            material.getInstrument() == null ? "" : material.getInstrument().getSymbol()
        );
    }

    private SourceRefResponse toSourceRef(PriceBar priceBar) {
        return new SourceRefResponse(
            priceBar.getId().toString(),
            priceBar.getInstrument().getSymbol() + " 일별 가격",
            "market_data",
            priceBar.getProvider(),
            priceBar.getTradeDate().atStartOfDay().atOffset(ZoneOffset.UTC),
            "",
            priceBar.getSourceKey(),
            priceBar.getInstrument().getSymbol()
        );
    }

    private List<SourceRefResponse> mergeSourceRefs(List<SourceRefResponse> first, List<SourceRefResponse> second) {
        Map<String, SourceRefResponse> byId = new LinkedHashMap<>();
        first.forEach(item -> byId.put(item.id(), item));
        second.forEach(item -> byId.putIfAbsent(item.id(), item));
        return List.copyOf(byId.values());
    }

    private List<String> sourceIds(List<SourceRefResponse> sourceRefs) {
        return sourceRefs.stream().map(SourceRefResponse::id).toList();
    }

    private List<MissingDataResponse> missingData(boolean hasData, String field, String expectedSource) {
        if (hasData) {
            return List.of();
        }
        return List.of(new MissingDataResponse(field, "백엔드에 저장된 원천 자료가 아직 부족합니다.", expectedSource));
    }

    private ConfidenceResponse confidence(boolean hasData) {
        if (hasData) {
            return new ConfidenceResponse(0.82, "high", "저장된 서버 데이터와 계산 결과를 기준으로 구성했습니다.");
        }
        return new ConfidenceResponse(0.22, "low", "저장된 서버 데이터가 부족해 화면 일부가 비어 있을 수 있습니다.");
    }

    private Map<String, Object> textBlock(String text, List<String> sourceRefIds) {
        return object("text", text, "sourceRefIds", sourceRefIds);
    }

    private Map<String, Object> object(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            map.put((String) pairs[index], pairs[index + 1]);
        }
        return map;
    }

    private String firstSummary(List<SourceMaterial> materials, String fallback) {
        return materials.stream()
            .map(this::summaryOrTitle)
            .filter(value -> !value.isBlank())
            .findFirst()
            .orElse(fallback);
    }

    private String summaryOrTitle(SourceMaterial material) {
        String summary = nullToEmpty(material.getSummary()).trim();
        return summary.isBlank() ? material.getTitle() : summary;
    }

    private OffsetDateTime publishedAt(SourceMaterial material) {
        return material.getPublishedAt() == null ? material.getFetchedAt() : material.getPublishedAt();
    }

    private boolean isDriverCandidate(SourceMaterial material) {
        return material.getKind() == SourceKind.NEWS ||
            material.getKind() == SourceKind.EARNINGS ||
            material.getKind() == SourceKind.FUNDAMENTALS;
    }

    private boolean isRiskCandidate(SourceMaterial material) {
        return material.getKind() == SourceKind.ECONOMIC ||
            material.getKind() == SourceKind.FED ||
            material.getKind() == SourceKind.DISCLOSURE;
    }

    private boolean isCalendarCandidate(SourceMaterial material) {
        return material.getKind() == SourceKind.EARNINGS ||
            material.getKind() == SourceKind.FED ||
            material.getKind() == SourceKind.DISCLOSURE ||
            material.getKind() == SourceKind.ECONOMIC;
    }

    private String sourceKind(SourceKind kind, boolean allowDisclosureKind) {
        return switch (kind) {
            case MARKET_DATA -> "market_data";
            case NEWS, MEDIA -> "news";
            case DISCLOSURE -> allowDisclosureKind ? "disclosure" : "fundamentals";
            case FUNDAMENTALS, EARNINGS -> "fundamentals";
            case ECONOMIC, FED -> "economic";
            case INTERNAL_CONFIG -> "internal_config";
        };
    }

    private String labelFromKind(SourceKind kind) {
        return switch (kind) {
            case MARKET_DATA -> "가격";
            case NEWS -> "뉴스";
            case DISCLOSURE -> "공시";
            case FUNDAMENTALS -> "기초자료";
            case ECONOMIC -> "경제";
            case EARNINGS -> "실적";
            case FED -> "연준";
            case MEDIA -> "미디어";
            case INTERNAL_CONFIG -> "설정";
        };
    }

    private String calendarCategory(SourceKind kind) {
        return switch (kind) {
            case EARNINGS -> "earnings";
            case DISCLOSURE -> "disclosure";
            case ECONOMIC, FED -> "macro";
            default -> "news";
        };
    }

    private String toneFromKind(SourceKind kind) {
        return switch (kind) {
            case DISCLOSURE, ECONOMIC, FED -> "neutral";
            case NEWS, EARNINGS, FUNDAMENTALS, MARKET_DATA, MEDIA, INTERNAL_CONFIG -> "positive";
        };
    }

    private String impactFromKind(SourceKind kind) {
        return switch (kind) {
            case DISCLOSURE -> "공시 확인 필요";
            case ECONOMIC, FED -> "거시 변수 확인";
            case EARNINGS -> "실적 영향 확인";
            case FUNDAMENTALS -> "기초 체력 확인";
            case MARKET_DATA -> "가격 변화 확인";
            case MEDIA -> "발표 자료 확인";
            case NEWS, INTERNAL_CONFIG -> "뉴스 영향 확인";
        };
    }

    private String marketGroup(SourceMaterial material) {
        if (material.getInstrument() == null) {
            return "global";
        }
        return "KR".equals(material.getInstrument().getMarket().name()) ? "domestic" : "watchlist";
    }

    private String normalizedSector(Instrument instrument) {
        return normalizedSector(instrument.getSector());
    }

    private String normalizedSector(String sector) {
        String value = nullToEmpty(sector).trim();
        return value.isBlank() ? "미분류" : value;
    }

    private String slug(String value) {
        return value.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9가-힣]+", "-")
            .replaceAll("(^-|-$)", "");
    }

    private double changePercentFromAscending(List<PriceBar> bars) {
        if (bars.size() < 2) {
            return 0.0;
        }
        PriceBar latest = bars.get(bars.size() - 1);
        PriceBar previous = bars.get(bars.size() - 2);
        return percentageChange(latest.getClosePrice(), previous.getClosePrice());
    }

    private double percentageChange(BigDecimal latest, BigDecimal previous) {
        double previousValue = toDouble(previous);
        if (previousValue == 0.0) {
            return 0.0;
        }
        return round((toDouble(latest) - previousValue) / previousValue * 100);
    }

    private double scoreFromChange(double changePercent, int dataCount) {
        double base = 55 + Math.max(-15, Math.min(15, changePercent * 2));
        double dataBonus = Math.min(20, dataCount);
        return Math.max(0, Math.min(100, base + dataBonus));
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private String rangeLabel(String range) {
        if (range == null || range.isBlank()) {
            return "최근 6개월";
        }
        return switch (range) {
            case "1m" -> "최근 1개월";
            case "3m" -> "최근 3개월";
            case "6m" -> "최근 6개월";
            case "1y" -> "최근 1년";
            default -> range;
        };
    }

    private List<Map<String, Object>> availableRanges() {
        return List.of(
            object("value", "1m", "label", "1개월"),
            object("value", "3m", "label", "3개월"),
            object("value", "6m", "label", "6개월"),
            object("value", "1y", "label", "1년")
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record InstrumentPrice(Instrument instrument, PriceBar latest, PriceBar previous) {
        boolean hasPrice() {
            return latest != null;
        }

        double changePercent() {
            if (latest == null || previous == null) {
                return 0.0;
            }
            double previousValue = previous.getClosePrice().doubleValue();
            if (previousValue == 0.0) {
                return 0.0;
            }
            return BigDecimal.valueOf((latest.getClosePrice().doubleValue() - previousValue) / previousValue * 100)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
        }
    }
}
