package com.alpaka.stock.api;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
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
class AuthAndSwaggerApiTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesSwaggerOpenApiDocument() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title", equalTo("Stock Research Backend API")))
            .andExpect(jsonPath("$.components.securitySchemes", hasKey("access-cookie")))
            .andExpect(jsonPath("$.components.securitySchemes", hasKey("refresh-cookie")));
    }

    @Test
    void issuesRefreshesAndClearsHttpOnlyCookieSession() throws Exception {
        MvcResult login = mockMvc.perform(post("/auth/dev-login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "email": "investor@example.com",
                      "displayName": "Investor",
                      "locale": "ko"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated", equalTo(true)))
            .andExpect(cookie().httpOnly("stock_access_token", true))
            .andExpect(cookie().httpOnly("stock_refresh_token", true))
            .andReturn();

        Cookie accessCookie = login.getResponse().getCookie("stock_access_token");
        Cookie refreshCookie = login.getResponse().getCookie("stock_refresh_token");

        mockMvc.perform(get("/auth/me").cookie(accessCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated", equalTo(true)))
            .andExpect(jsonPath("$.email", equalTo("investor@example.com")));

        MvcResult refreshed = mockMvc.perform(post("/auth/refresh")
                .with(csrf())
                .cookie(refreshCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated", equalTo(true)))
            .andExpect(cookie().httpOnly("stock_access_token", true))
            .andExpect(cookie().httpOnly("stock_refresh_token", true))
            .andReturn();

        Cookie rotatedRefreshCookie = refreshed.getResponse().getCookie("stock_refresh_token");

        mockMvc.perform(post("/auth/logout")
                .with(csrf())
                .cookie(rotatedRefreshCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.loggedOut", equalTo(true)))
            .andExpect(cookie().maxAge("stock_access_token", 0))
            .andExpect(cookie().maxAge("stock_refresh_token", 0));
    }
}
