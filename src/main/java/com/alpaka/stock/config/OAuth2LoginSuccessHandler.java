package com.alpaka.stock.config;

import com.alpaka.stock.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthService authService;
    private final String frontendCallbackUrl;

    public OAuth2LoginSuccessHandler(
        AuthService authService,
        @Value("${stock.auth.frontend-callback-url}") String frontendCallbackUrl
    ) {
        this.authService = authService;
        this.frontendCallbackUrl = frontendCallbackUrl;
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User principal = (OAuth2User) authentication.getPrincipal();
        String email = attribute(principal, "email", attribute(principal, "sub", "unknown@example.com"));
        String displayName = attribute(principal, "name", email);
        String providerUserId = attribute(principal, "sub", email);
        String locale = attribute(principal, "locale", "ko");
        authService.login("google", providerUserId, email, displayName, locale, request, response);
        response.sendRedirect(frontendCallbackUrl);
    }

    private String attribute(OAuth2User principal, String key, String fallback) {
        Object value = principal.getAttribute(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return String.valueOf(value);
    }
}
