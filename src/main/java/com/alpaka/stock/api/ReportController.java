package com.alpaka.stock.api;

import com.alpaka.stock.api.dto.ReportDtos.ReportDeliveryListResponse;
import com.alpaka.stock.api.dto.ReportDtos.ReportDeliveryResponse;
import com.alpaka.stock.api.dto.ReportDtos.ReportPreviewRequest;
import com.alpaka.stock.api.dto.ReportDtos.ReportPreviewResponse;
import com.alpaka.stock.api.dto.ReportDtos.ReportSendRequest;
import com.alpaka.stock.service.ReportDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"", "/api/v1"})
@Tag(name = "Reports", description = "저장된 시장 자료 기반 리포트 미리보기와 이메일 발송 API")
public class ReportController {
    private final ReportDeliveryService reportDeliveryService;

    public ReportController(ReportDeliveryService reportDeliveryService) {
        this.reportDeliveryService = reportDeliveryService;
    }

    @PostMapping("/reports/preview")
    @Operation(summary = "리포트 미리보기", description = "서버에 저장된 가격, 뉴스, 공시를 사용해 이메일 본문을 만듭니다.")
    public ReportPreviewResponse preview(@Valid @RequestBody ReportPreviewRequest request) {
        return reportDeliveryService.preview(request);
    }

    @PostMapping("/reports/send")
    @Operation(summary = "리포트 이메일 발송", description = "SMTP가 활성화되어 있으면 이메일을 보내고, 발송 이력을 DB에 저장합니다.")
    public ReportDeliveryResponse send(@Valid @RequestBody ReportSendRequest request) {
        return reportDeliveryService.send(request);
    }

    @GetMapping("/reports")
    @Operation(summary = "리포트 발송 이력 조회", description = "사용자별 최근 리포트 생성/발송 이력을 조회합니다.")
    public ReportDeliveryListResponse deliveries(@RequestParam String userId) {
        return new ReportDeliveryListResponse(reportDeliveryService.deliveries(userId));
    }
}
