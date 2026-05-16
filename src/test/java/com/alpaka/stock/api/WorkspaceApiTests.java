package com.alpaka.stock.api;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkspaceApiTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void storesMaterialsAndServesOverviewFromBackend() throws Exception {
        mockMvc.perform(post("/instruments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "symbol": "NVDA",
                      "name": "NVIDIA",
                      "market": "US",
                      "exchange": "NASDAQ",
                      "securityCode": "NVDA-US",
                      "sector": "Semiconductors",
                      "currency": "USD",
                      "active": true
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
                      "title": "NVIDIA earnings preview",
                      "summary": "Earnings preview stored by backend.",
                      "sourceUrl": "https://example.com/nvda",
                      "sourceKey": "alpha:news:nvda:1",
                      "language": "en",
                      "publishedAt": "2026-05-16T00:00:00Z",
                      "rawPayload": "{\\"symbol\\":\\"NVDA\\"}"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.symbol", equalTo("NVDA")));

        mockMvc.perform(get("/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sourceRefs", hasSize(1)))
            .andExpect(jsonPath("$.storedCounts.news", equalTo(1)));
    }

    @Test
    void storesResearchSnapshotsWithInstrumentRelation() throws Exception {
        mockMvc.perform(post("/instruments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "symbol": "005930",
                      "name": "삼성전자",
                      "market": "KR",
                      "exchange": "KRX",
                      "securityCode": "005930-KR",
                      "sector": "반도체",
                      "currency": "KRW",
                      "active": true
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/snapshots")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "userId": "local-user",
                      "symbol": "005930",
                      "note": "실적 발표 전 확인",
                      "stance": "watch",
                      "conviction": "medium",
                      "thesis": "메모리 가격과 환율을 함께 확인",
                      "price": 76000,
                      "changePercent": 1.2,
                      "score": 68,
                      "activeRuleLabels": ["20일선 회복"],
                      "presetName": "기본"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.snapshot.symbol", equalTo("005930")));

        mockMvc.perform(get("/snapshots").param("symbol", "005930"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.snapshots", hasSize(1)))
            .andExpect(jsonPath("$.snapshots[0].name", equalTo("삼성전자")));
    }
}
