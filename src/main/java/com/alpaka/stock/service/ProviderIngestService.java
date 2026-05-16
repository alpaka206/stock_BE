package com.alpaka.stock.service;

import com.alpaka.stock.api.dto.MaterialDtos.MaterialIngestRequest;
import com.alpaka.stock.api.dto.ProviderDtos.AlphaDailyIngestRequest;
import com.alpaka.stock.api.dto.ProviderDtos.AlphaNewsIngestRequest;
import com.alpaka.stock.api.dto.ProviderDtos.IngestRunResponse;
import com.alpaka.stock.api.dto.ProviderDtos.OpenDartDisclosureIngestRequest;
import com.alpaka.stock.api.dto.ProviderDtos.SecSubmissionIngestRequest;
import com.alpaka.stock.api.dto.PriceDtos.PriceBarUpsertRequest;
import com.alpaka.stock.domain.Instrument;
import com.alpaka.stock.domain.Market;
import com.alpaka.stock.domain.SourceKind;
import com.alpaka.stock.repository.InstrumentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ProviderIngestService {
    private static final String ALPHA_PROVIDER = "Alpha Vantage";
    private static final String OPENDART_PROVIDER = "OpenDART";
    private static final String SEC_PROVIDER = "SEC EDGAR";
    private static final DateTimeFormatter ALPHA_NEWS_TIME_FORMAT =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss", Locale.ROOT);
    private static final DateTimeFormatter DART_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final MaterialService materialService;
    private final PriceBarService priceBarService;
    private final InstrumentRepository instrumentRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String alphaVantageBaseUrl;
    private final String alphaVantageApiKey;
    private final String openDartBaseUrl;
    private final String openDartApiKey;
    private final String secBaseUrl;
    private final String secUserAgent;

    public ProviderIngestService(
        MaterialService materialService,
        PriceBarService priceBarService,
        InstrumentRepository instrumentRepository,
        ObjectMapper objectMapper,
        RestClient.Builder restClientBuilder,
        @Value("${stock.providers.alpha-vantage.base-url}") String alphaVantageBaseUrl,
        @Value("${stock.providers.alpha-vantage.api-key}") String alphaVantageApiKey,
        @Value("${stock.providers.open-dart.base-url}") String openDartBaseUrl,
        @Value("${stock.providers.open-dart.api-key}") String openDartApiKey,
        @Value("${stock.providers.sec.base-url}") String secBaseUrl,
        @Value("${stock.providers.sec.user-agent}") String secUserAgent
    ) {
        this.materialService = materialService;
        this.priceBarService = priceBarService;
        this.instrumentRepository = instrumentRepository;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
        this.alphaVantageBaseUrl = alphaVantageBaseUrl;
        this.alphaVantageApiKey = alphaVantageApiKey;
        this.openDartBaseUrl = openDartBaseUrl;
        this.openDartApiKey = openDartApiKey;
        this.secBaseUrl = secBaseUrl;
        this.secUserAgent = secUserAgent;
    }

    @Transactional
    public IngestRunResponse ingestAlphaDaily(AlphaDailyIngestRequest request) {
        requireApiKey(alphaVantageApiKey, "ALPHA_VANTAGE_API_KEY");
        String symbol = normalizeSymbol(request.symbol());
        ensureUsInstrument(symbol);
        String outputSize = request.outputSizeOrDefault() > 100 ? "full" : "compact";
        URI uri = UriComponentsBuilder.fromUriString(alphaVantageBaseUrl)
            .queryParam("function", "TIME_SERIES_DAILY")
            .queryParam("symbol", symbol)
            .queryParam("outputsize", outputSize)
            .queryParam("apikey", alphaVantageApiKey)
            .build(true)
            .toUri();
        JsonNode payload = getJson(uri);
        JsonNode series = payload.get("Time Series (Daily)");
        if (series == null || !series.isObject()) {
            throw providerError(ALPHA_PROVIDER, payload);
        }

        int requested = 0;
        int stored = 0;
        List<String> sourceKeys = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = series.fields();
        while (fields.hasNext() && requested < request.outputSizeOrDefault()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            LocalDate tradeDate = LocalDate.parse(entry.getKey());
            JsonNode bar = entry.getValue();
            String sourceKey = "alpha-vantage:daily:%s:%s".formatted(symbol, tradeDate);
            priceBarService.upsert(new PriceBarUpsertRequest(
                symbol,
                tradeDate,
                decimal(bar, "1. open"),
                decimal(bar, "2. high"),
                decimal(bar, "3. low"),
                decimal(bar, "4. close"),
                decimal(bar, "5. volume"),
                ALPHA_PROVIDER,
                sourceKey
            ));
            requested += 1;
            stored += 1;
            sourceKeys.add(sourceKey);
        }

        return new IngestRunResponse(
            ALPHA_PROVIDER,
            "daily-price",
            requested,
            stored,
            Math.max(0, requested - stored),
            "일봉 가격을 서버 저장소에 반영했습니다.",
            sourceKeys
        );
    }

    @Transactional
    public IngestRunResponse ingestAlphaNews(AlphaNewsIngestRequest request) {
        requireApiKey(alphaVantageApiKey, "ALPHA_VANTAGE_API_KEY");
        String tickers = request.tickers().trim().toUpperCase(Locale.ROOT);
        URI uri = UriComponentsBuilder.fromUriString(alphaVantageBaseUrl)
            .queryParam("function", "NEWS_SENTIMENT")
            .queryParam("tickers", tickers)
            .queryParam("limit", request.limitOrDefault())
            .queryParam("apikey", alphaVantageApiKey)
            .build(true)
            .toUri();
        JsonNode payload = getJson(uri);
        JsonNode feed = payload.get("feed");
        if (feed == null || !feed.isArray()) {
            throw providerError(ALPHA_PROVIDER, payload);
        }

        int requested = 0;
        int stored = 0;
        List<String> sourceKeys = new ArrayList<>();
        for (JsonNode item : feed) {
            if (requested >= request.limitOrDefault()) {
                break;
            }
            String title = text(item, "title", "제목 없음");
            String url = text(item, "url", "");
            String sourceKey = sourceKey("alpha-vantage:news", url.isBlank() ? title : url);
            materialService.ingest(new MaterialIngestRequest(
                firstTicker(tickers),
                SourceKind.NEWS,
                ALPHA_PROVIDER,
                text(item, "source", ALPHA_PROVIDER),
                title,
                text(item, "summary", ""),
                url,
                sourceKey,
                "en",
                parseAlphaNewsTime(text(item, "time_published", "")),
                item.toString()
            ));
            requested += 1;
            stored += 1;
            sourceKeys.add(sourceKey);
        }

        return new IngestRunResponse(
            ALPHA_PROVIDER,
            "news",
            requested,
            stored,
            Math.max(0, requested - stored),
            "뉴스를 서버 저장소에 반영했습니다.",
            sourceKeys
        );
    }

    @Transactional
    public IngestRunResponse ingestOpenDartDisclosures(OpenDartDisclosureIngestRequest request) {
        requireApiKey(openDartApiKey, "OPENDART_API_KEY");
        LocalDate beginDate = request.beginDateOrDefault();
        LocalDate endDate = request.endDateOrDefault();
        URI uri = UriComponentsBuilder.fromUriString(openDartBaseUrl.replaceAll("/$", "") + "/list.json")
            .queryParam("crtfc_key", openDartApiKey)
            .queryParam("corp_code", request.corpCode())
            .queryParam("bgn_de", beginDate.format(DART_DATE_FORMAT))
            .queryParam("end_de", endDate.format(DART_DATE_FORMAT))
            .queryParam("page_count", request.limitOrDefault())
            .build(true)
            .toUri();
        JsonNode payload = getJson(uri);
        if (!"000".equals(text(payload, "status", ""))) {
            throw providerError(OPENDART_PROVIDER, payload);
        }

        JsonNode list = payload.get("list");
        int requested = 0;
        int stored = 0;
        List<String> sourceKeys = new ArrayList<>();
        if (list != null && list.isArray()) {
            for (JsonNode item : list) {
                if (requested >= request.limitOrDefault()) {
                    break;
                }
                String receiptNo = text(item, "rcept_no", "");
                String sourceKey = sourceKey("opendart:list", receiptNo);
                materialService.ingest(new MaterialIngestRequest(
                    request.symbol(),
                    SourceKind.DISCLOSURE,
                    OPENDART_PROVIDER,
                    text(item, "corp_name", OPENDART_PROVIDER),
                    text(item, "report_nm", "공시"),
                    text(item, "flr_nm", ""),
                    receiptNo.isBlank() ? "" : "https://dart.fss.or.kr/dsaf001/main.do?rcpNo=" + receiptNo,
                    sourceKey,
                    "ko",
                    parseDartDate(text(item, "rcept_dt", "")),
                    item.toString()
                ));
                requested += 1;
                stored += 1;
                sourceKeys.add(sourceKey);
            }
        }

        return new IngestRunResponse(
            OPENDART_PROVIDER,
            "disclosure",
            requested,
            stored,
            Math.max(0, requested - stored),
            "국내 공시를 서버 저장소에 반영했습니다.",
            sourceKeys
        );
    }

    @Transactional
    public IngestRunResponse ingestSecSubmissions(SecSubmissionIngestRequest request) {
        String cik = normalizeCik(request.cik());
        URI uri = UriComponentsBuilder.fromUriString(secBaseUrl.replaceAll("/$", "") + "/submissions/CIK" + cik + ".json")
            .build(true)
            .toUri();
        JsonNode payload = restClient.get()
            .uri(uri)
            .header("User-Agent", secUserAgent)
            .retrieve()
            .body(JsonNode.class);
        if (payload == null || payload.get("filings") == null) {
            throw providerError(SEC_PROVIDER, objectMapper.createObjectNode().put("message", "SEC 응답에 filings가 없습니다."));
        }

        JsonNode recent = payload.path("filings").path("recent");
        JsonNode forms = recent.path("form");
        JsonNode accessionNumbers = recent.path("accessionNumber");
        JsonNode filingDates = recent.path("filingDate");
        JsonNode reportDates = recent.path("reportDate");
        JsonNode primaryDocuments = recent.path("primaryDocument");
        int requested = 0;
        int stored = 0;
        List<String> sourceKeys = new ArrayList<>();
        int max = Math.min(forms.size(), request.limitOrDefault());
        for (int index = 0; index < max; index += 1) {
            String accessionNumber = accessionNumbers.path(index).asText("");
            String filingDate = filingDates.path(index).asText("");
            String primaryDocument = primaryDocuments.path(index).asText("");
            String sourceKey = sourceKey("sec:submission", accessionNumber);
            String archiveUrl = buildSecArchiveUrl(cik, accessionNumber, primaryDocument);
            materialService.ingest(new MaterialIngestRequest(
                request.symbol(),
                SourceKind.DISCLOSURE,
                SEC_PROVIDER,
                text(payload, "name", SEC_PROVIDER),
                "%s %s".formatted(forms.path(index).asText("Filing"), filingDate).trim(),
                "reportDate=%s, accessionNumber=%s".formatted(reportDates.path(index).asText(""), accessionNumber),
                archiveUrl,
                sourceKey,
                "en",
                parseIsoDate(filingDate),
                objectMapper.createObjectNode()
                    .put("form", forms.path(index).asText(""))
                    .put("accessionNumber", accessionNumber)
                    .put("filingDate", filingDate)
                    .put("reportDate", reportDates.path(index).asText(""))
                    .put("primaryDocument", primaryDocument)
                    .toString()
            ));
            requested += 1;
            stored += 1;
            sourceKeys.add(sourceKey);
        }

        return new IngestRunResponse(
            SEC_PROVIDER,
            "submission",
            requested,
            stored,
            Math.max(0, requested - stored),
            "미국 SEC 공시를 서버 저장소에 반영했습니다.",
            sourceKeys
        );
    }

    private JsonNode getJson(URI uri) {
        JsonNode response = restClient.get().uri(uri).retrieve().body(JsonNode.class);
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "provider 응답이 비어 있습니다.");
        }
        return response;
    }

    private void requireApiKey(String value, String envName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.PRECONDITION_REQUIRED,
                envName + "가 설정되지 않았습니다. 실제 provider 키를 서버 환경변수로 넣어 주세요."
            );
        }
    }

    private ResponseStatusException providerError(String provider, JsonNode payload) {
        String message = text(payload, "Error Message", text(payload, "Information", text(payload, "message", payload.toString())));
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, provider + " 응답을 저장할 수 없습니다: " + message);
    }

    private void ensureUsInstrument(String symbol) {
        if (instrumentRepository.findFirstBySymbolIgnoreCase(symbol).isPresent()) {
            return;
        }
        instrumentRepository.save(new Instrument(symbol, symbol, Market.US, "US", symbol, null, "USD"));
    }

    private String normalizeSymbol(String symbol) {
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCik(String rawCik) {
        String digits = rawCik.replaceAll("\\D", "");
        if (digits.isBlank() || digits.length() > 10) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CIK는 10자리 이하 숫자여야 합니다.");
        }
        return "0".repeat(10 - digits.length()) + digits;
    }

    private String buildSecArchiveUrl(String cik, String accessionNumber, String primaryDocument) {
        if (accessionNumber.isBlank() || primaryDocument.isBlank()) {
            return "";
        }
        String cikWithoutLeadingZeros = cik.replaceFirst("^0+", "");
        return "https://www.sec.gov/Archives/edgar/data/%s/%s/%s".formatted(
            cikWithoutLeadingZeros,
            accessionNumber.replace("-", ""),
            primaryDocument
        );
    }

    private BigDecimal decimal(JsonNode node, String field) {
        return new BigDecimal(node.path(field).asText("0"));
    }

    private String text(JsonNode node, String field, String fallback) {
        if (node == null || node.get(field) == null || node.get(field).isNull()) {
            return fallback;
        }
        String value = node.get(field).asText();
        return value == null || value.isBlank() ? fallback : value;
    }

    private OffsetDateTime parseAlphaNewsTime(String value) {
        if (value == null || value.isBlank()) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
        return OffsetDateTime.of(java.time.LocalDateTime.parse(value, ALPHA_NEWS_TIME_FORMAT), ZoneOffset.UTC);
    }

    private OffsetDateTime parseDartDate(String value) {
        if (value == null || value.isBlank()) {
            return OffsetDateTime.now(ZoneOffset.ofHours(9));
        }
        return OffsetDateTime.of(LocalDate.parse(value, DART_DATE_FORMAT), LocalTime.NOON, ZoneOffset.ofHours(9));
    }

    private OffsetDateTime parseIsoDate(String value) {
        if (value == null || value.isBlank()) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
        return OffsetDateTime.of(LocalDate.parse(value), LocalTime.NOON, ZoneOffset.UTC);
    }

    private String firstTicker(String tickers) {
        return tickers.split(",", 2)[0].trim();
    }

    private String sourceKey(String prefix, String value) {
        String cleaned = value == null || value.isBlank() ? "unknown" : value.trim();
        String key = prefix + ":" + cleaned;
        return key.length() <= 220 ? key : key.substring(0, 220);
    }
}
