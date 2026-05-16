package com.alpaka.stock.api;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkspaceApiTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesCorePageContractsFromStoredBackendData() throws Exception {
        upsertInstrument("NVDA", "NVIDIA", "US", "NASDAQ", "NVDA-US", "Semiconductors", "USD");
        upsertInstrument("005930", "삼성전자", "KR", "KRX", "005930-KR", "Semiconductors", "KRW");
        upsertPrice("NVDA", "2026-05-15", 900, 920, 890, 910, 1200000, "Alpha Vantage", "alpha:daily:nvda:1");
        upsertPrice("NVDA", "2026-05-16", 910, 950, 905, 940, 1500000, "Alpha Vantage", "alpha:daily:nvda:2");
        upsertPrice("005930", "2026-05-15", 76000, 77000, 75500, 76500, 10000000, "KIS", "kis:daily:005930:1");
        upsertPrice("005930", "2026-05-16", 76500, 79000, 76000, 78500, 13000000, "KIS", "kis:daily:005930:2");

        mockMvc.perform(post("/materials")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "symbol": "NVDA",
                      "kind": "NEWS",
                      "provider": "Alpha Vantage",
                      "publisher": "Reuters",
                      "title": "NVIDIA earnings preview",
                      "summary": "Earnings preview stored by backend.",
                      "sourceUrl": "https://example.com/nvda",
                      "sourceKey": "alpha:news:nvda:contract",
                      "language": "en",
                      "publishedAt": "2026-05-16T00:00:00Z",
                      "rawPayload": "{\\"symbol\\":\\"NVDA\\"}"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol", equalTo("NVDA")));

        mockMvc.perform(post("/materials")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "symbol": "NVDA",
                      "kind": "NEWS",
                      "provider": "Alpha Vantage",
                      "publisher": "Reuters",
                      "title": "Undated NVIDIA brief",
                      "summary": "Undated material should not hide dated news.",
                      "sourceUrl": "https://example.com/nvda-undated",
                      "sourceKey": "alpha:news:nvda:undated",
                      "language": "en",
                      "rawPayload": "{\\"symbol\\":\\"NVDA\\"}"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol", equalTo("NVDA")));

        mockMvc.perform(get("/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourceRefs", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.marketSummary.text", equalTo("Earnings preview stored by backend.")))
            .andExpect(jsonPath("$.notableNews", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.notableNews[0].headline", equalTo("NVIDIA earnings preview")));

        mockMvc.perform(get("/radar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.watchlistRows", hasSize(greaterThanOrEqualTo(2))))
            .andExpect(jsonPath("$.folderTree", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.alertRules", hasSize(3)));

        mockMvc.perform(get("/stocks/NVDA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.instrument.symbol", equalTo("NVDA")))
            .andExpect(jsonPath("$.latestPrice", equalTo(940.0)))
            .andExpect(jsonPath("$.priceSeries", hasSize(2)))
            .andExpect(jsonPath("$.issueCards", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$.issueCards[0].title", equalTo("NVIDIA earnings preview")));

        mockMvc.perform(get("/history").param("symbol", "NVDA").param("range", "3m"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol", equalTo("NVDA")))
            .andExpect(jsonPath("$.priceSeries", hasSize(2)))
            .andExpect(jsonPath("$.eventTimeline", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/news"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.featuredNews", hasSize(greaterThanOrEqualTo(1))));

        mockMvc.perform(get("/calendar"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.calendarSummary.text").isString());
    }

    @Test
    void storesResearchSnapshotsAndPlatformJobs() throws Exception {
        mockMvc.perform(get("/csrf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.headerName").isString());

        upsertInstrument("TSLA", "Tesla", "US", "NASDAQ", "TSLA-US", "Automobiles", "USD");

        mockMvc.perform(post("/snapshots")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "local-user",
                      "symbol": "TSLA",
                      "note": "실적 발표 전 가격 반응 확인",
                      "stance": "watch",
                      "conviction": "medium",
                      "thesis": "가격과 이벤트를 함께 확인",
                      "price": 180,
                      "changePercent": 1.2,
                      "score": 68,
                      "activeRuleLabels": ["20일선 회복"],
                      "presetName": "기본"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.snapshot.symbol", equalTo("TSLA")));

        mockMvc.perform(get("/snapshots").param("symbol", "TSLA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.snapshots", hasSize(1)))
            .andExpect(jsonPath("$.snapshots[0].name", equalTo("Tesla")));

        mockMvc.perform(get("/subscription-plans"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.plans", hasSize(3)))
            .andExpect(jsonPath("$.plans[0].code", equalTo("free")));

        mockMvc.perform(post("/report-schedules")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "local-user",
                      "locale": "ko",
                      "cadence": "WEEKLY",
                      "deliveryEmail": "user@example.com",
                      "timezone": "Asia/Seoul"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deliveryEmail", equalTo("user@example.com")))
            .andExpect(jsonPath("$.enabled", equalTo(true)));

        mockMvc.perform(post("/reports/preview")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "local-user",
                      "deliveryEmail": "user@example.com",
                      "locale": "ko",
                      "cadence": "WEEKLY",
                      "symbols": ["TSLA"]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.subject").isString())
            .andExpect(jsonPath("$.textBody").isString());

        mockMvc.perform(post("/reports/send")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "local-user",
                      "deliveryEmail": "user@example.com",
                      "locale": "ko",
                      "cadence": "WEEKLY",
                      "symbols": ["TSLA"]
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", equalTo("READY")));

        mockMvc.perform(get("/reports").param("userId", "local-user"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deliveries", hasSize(1)));

        MvcResult mediaResult = mockMvc.perform(post("/media-assets")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "symbol": "TSLA",
                      "kind": "VIDEO",
                      "title": "Tesla earnings call",
                      "sourceUrl": "https://example.com/tesla-call",
                      "provider": "Investor Relations",
                      "language": "en",
                      "publishedAt": "2026-05-16T00:00:00Z"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol", equalTo("TSLA")))
            .andReturn();

        String mediaId = com.jayway.jsonpath.JsonPath.read(
            mediaResult.getResponse().getContentAsString(),
            "$.id"
        );

        MvcResult localizationResult = mockMvc.perform(post("/localization-jobs")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "mediaAssetId": "%s",
                      "provider": "Perso",
                      "targetLanguage": "ko"
                    }
                    """.formatted(mediaId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", equalTo("REQUESTED")))
            .andReturn();

        String localizationJobId = com.jayway.jsonpath.JsonPath.read(
            localizationResult.getResponse().getContentAsString(),
            "$.id"
        );

        mockMvc.perform(post("/localization-jobs/%s/submit".formatted(localizationJobId))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().is(428))
            .andExpect(jsonPath("$.message").isString());

        mockMvc.perform(post("/provider-ingest/alpha-vantage/daily")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "symbol": "NVDA",
                      "outputSize": 5
                    }
                    """))
            .andExpect(status().is(428))
            .andExpect(jsonPath("$.message").isString());

        mockMvc.perform(post("/automation/webhooks/n8n")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "eventType": "daily-report-ready",
                      "payload": {
                        "symbol": "TSLA"
                      }
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.source", equalTo("n8n")))
            .andExpect(jsonPath("$.status", equalTo("received")));
    }

    private void upsertInstrument(
        String symbol,
        String name,
        String market,
        String exchange,
        String securityCode,
        String sector,
        String currency
    ) throws Exception {
        mockMvc.perform(post("/instruments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "symbol": "%s",
                      "name": "%s",
                      "market": "%s",
                      "exchange": "%s",
                      "securityCode": "%s",
                      "sector": "%s",
                      "currency": "%s",
                      "active": true
                    }
                    """.formatted(symbol, name, market, exchange, securityCode, sector, currency)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol", equalTo(symbol)));
    }

    private void upsertPrice(
        String symbol,
        String tradeDate,
        double open,
        double high,
        double low,
        double close,
        double volume,
        String provider,
        String sourceKey
    ) throws Exception {
        mockMvc.perform(post("/prices/bars")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "symbol": "%s",
                      "tradeDate": "%s",
                      "openPrice": %s,
                      "highPrice": %s,
                      "lowPrice": %s,
                      "closePrice": %s,
                      "volume": %s,
                      "provider": "%s",
                      "sourceKey": "%s"
                    }
                    """.formatted(symbol, tradeDate, open, high, low, close, volume, provider, sourceKey)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol", equalTo(symbol)));
    }
}
