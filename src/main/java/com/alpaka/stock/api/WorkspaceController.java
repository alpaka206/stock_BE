package com.alpaka.stock.api;

import com.alpaka.stock.service.ContractViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"", "/api/v1"})
@Tag(name = "Research Views", description = "프런트 화면이 바로 사용하는 저장 데이터 기반 화면 API")
public class WorkspaceController {
    private final ContractViewService contractViewService;

    public WorkspaceController(ContractViewService contractViewService) {
        this.contractViewService = contractViewService;
    }

    @GetMapping("/overview")
    @Operation(summary = "시장 개요", description = "저장된 가격, 뉴스, 공시 자료로 시장 개요 화면 계약을 반환합니다.")
    public Map<String, Object> overview() {
        return contractViewService.overview();
    }

    @GetMapping("/radar")
    @Operation(summary = "관심 종목 레이더", description = "저장 종목과 가격 자료로 레이더 화면 계약을 반환합니다.")
    public Map<String, Object> radar() {
        return contractViewService.radar();
    }

    @GetMapping("/stocks/{symbol}")
    @Operation(summary = "종목 상세", description = "종목별 가격, 이벤트, 기술적 지표, 이슈 카드 계약을 반환합니다.")
    public Map<String, Object> stock(@PathVariable String symbol) {
        return contractViewService.stock(symbol);
    }

    @GetMapping("/history")
    @Operation(summary = "이벤트 히스토리", description = "가격 기록과 원천 자료를 연결해 과거 이벤트 복기 화면 계약을 반환합니다.")
    public Map<String, Object> history(
        @RequestParam(required = false) String symbol,
        @RequestParam(required = false) String range
    ) {
        return contractViewService.history(symbol, range);
    }

    @GetMapping("/news")
    @Operation(summary = "뉴스와 공시", description = "저장된 뉴스, 공시, 실적, 경제 자료를 뉴스 화면 계약으로 반환합니다.")
    public Map<String, Object> news() {
        return contractViewService.news();
    }

    @GetMapping("/calendar")
    @Operation(summary = "시장 일정", description = "저장 자료 중 일정성 이벤트를 캘린더 화면 계약으로 반환합니다.")
    public Map<String, Object> calendar() {
        return contractViewService.calendar();
    }
}
