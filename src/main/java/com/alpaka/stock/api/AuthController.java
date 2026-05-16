package com.alpaka.stock.api;

import com.alpaka.stock.api.dto.AuthDtos.DevLoginRequest;
import com.alpaka.stock.api.dto.AuthDtos.LogoutResponse;
import com.alpaka.stock.api.dto.AuthDtos.UserSessionResponse;
import com.alpaka.stock.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"", "/api/v1"})
@Tag(name = "Auth", description = "Google 로그인 이후 access/refresh cookie 세션을 관리합니다.")
public class AuthController {
    private final AuthService authService;
    private final boolean devLoginEnabled;

    public AuthController(
        AuthService authService,
        @Value("${stock.auth.dev-login-enabled}") boolean devLoginEnabled
    ) {
        this.authService = authService;
        this.devLoginEnabled = devLoginEnabled;
    }

    @PostMapping("/auth/dev-login")
    @Operation(summary = "개발용 로그인", description = "운영에서는 비활성화하고 Google OAuth 성공 콜백으로만 세션을 발급합니다.")
    public UserSessionResponse devLogin(
        @Valid @RequestBody DevLoginRequest request,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        if (!devLoginEnabled) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String displayName = request.displayName() == null || request.displayName().isBlank()
            ? request.email()
            : request.displayName();
        return authService.login(
            "dev",
            request.email(),
            request.email(),
            displayName,
            request.locale(),
            servletRequest,
            servletResponse
        );
    }

    @PostMapping("/auth/refresh")
    @Operation(summary = "access token 재발급", description = "refresh cookie를 검증하고 refresh token을 회전합니다.")
    public UserSessionResponse refresh(
        @CookieValue(name = "${stock.auth.refresh-cookie-name}", required = false) String refreshToken,
        HttpServletRequest servletRequest,
        HttpServletResponse servletResponse
    ) {
        return authService.refresh(refreshToken, servletRequest, servletResponse);
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "로그아웃", description = "현재 refresh session을 폐기하고 인증 cookie를 제거합니다.")
    public LogoutResponse logout(
        @CookieValue(name = "${stock.auth.refresh-cookie-name}", required = false) String refreshToken,
        HttpServletResponse servletResponse
    ) {
        authService.logout(refreshToken, servletResponse);
        return new LogoutResponse(true);
    }

    @GetMapping("/auth/me")
    @Operation(summary = "현재 세션 조회", description = "access cookie가 유효하면 사용자 정보를 반환합니다.")
    public UserSessionResponse me(HttpServletRequest request) {
        return authService.currentUser(readCookie(request, authService.accessCookieName()));
    }

    private String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
            .filter(cookie -> cookie.getName().equals(name))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }
}
