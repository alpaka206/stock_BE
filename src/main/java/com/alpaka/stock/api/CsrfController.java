package com.alpaka.stock.api;

import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"", "/api/v1"})
@Tag(name = "Security", description = "CSRF와 브라우저 보안 보조 API")
public class CsrfController {
    @GetMapping("/csrf")
    @Operation(summary = "CSRF 토큰 조회", description = "프런트 BFF가 POST/DELETE 요청 전에 사용할 CSRF 토큰을 받습니다.")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of(
            "headerName", token.getHeaderName(),
            "parameterName", token.getParameterName(),
            "token", token.getToken()
        );
    }
}
