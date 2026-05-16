package com.alpaka.stock.api;

import com.alpaka.stock.api.dto.PlatformDtos.AutomationWebhookRequest;
import com.alpaka.stock.api.dto.PlatformDtos.AutomationWebhookResponse;
import com.alpaka.stock.api.dto.PlatformDtos.LocalizationJobListResponse;
import com.alpaka.stock.api.dto.PlatformDtos.LocalizationJobRequest;
import com.alpaka.stock.api.dto.PlatformDtos.LocalizationJobResponse;
import com.alpaka.stock.api.dto.PlatformDtos.LocalizationJobSubmitRequest;
import com.alpaka.stock.api.dto.PlatformDtos.MediaAssetListResponse;
import com.alpaka.stock.api.dto.PlatformDtos.MediaAssetRequest;
import com.alpaka.stock.api.dto.PlatformDtos.MediaAssetResponse;
import com.alpaka.stock.api.dto.PlatformDtos.ReportScheduleListResponse;
import com.alpaka.stock.api.dto.PlatformDtos.ReportScheduleRequest;
import com.alpaka.stock.api.dto.PlatformDtos.ReportScheduleResponse;
import com.alpaka.stock.api.dto.PlatformDtos.SubscriptionPlanListResponse;
import com.alpaka.stock.service.PersoLocalizationService;
import com.alpaka.stock.service.PlatformService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"", "/api/v1"})
@Tag(name = "Platform", description = "구독, 리포트, 미디어, 현지화, 자동화 저장 API")
public class PlatformController {
    private final PlatformService platformService;
    private final PersoLocalizationService persoLocalizationService;

    public PlatformController(PlatformService platformService, PersoLocalizationService persoLocalizationService) {
        this.platformService = platformService;
        this.persoLocalizationService = persoLocalizationService;
    }

    @GetMapping("/subscription-plans")
    @Operation(summary = "구독 플랜 조회", description = "접근 제한은 아직 강제하지 않고, 요금제 UI가 사용할 기능 제한만 내려줍니다.")
    public SubscriptionPlanListResponse subscriptionPlans() {
        return new SubscriptionPlanListResponse(platformService.subscriptionPlans());
    }

    @GetMapping("/report-schedules")
    @Operation(summary = "리포트 예약 조회", description = "사용자별 오늘/이번 주 리포트 발송 예약을 조회합니다.")
    public ReportScheduleListResponse reportSchedules(@RequestParam String userId) {
        return new ReportScheduleListResponse(platformService.reportSchedules(userId));
    }

    @PostMapping("/report-schedules")
    @Operation(summary = "리포트 예약 저장", description = "리포트 cadence, 언어, 이메일, 시간대를 서버에 저장합니다.")
    public ReportScheduleResponse createReportSchedule(@Valid @RequestBody ReportScheduleRequest request) {
        return platformService.createReportSchedule(request);
    }

    @GetMapping("/media-assets")
    @Operation(summary = "미디어 자료 조회", description = "어닝콜, 연준 발표, 실적 발표 오디오/영상 자료를 조회합니다.")
    public MediaAssetListResponse mediaAssets() {
        return new MediaAssetListResponse(platformService.mediaAssets());
    }

    @PostMapping("/media-assets")
    @Operation(summary = "미디어 자료 저장", description = "Perso 더빙/자막 작업의 원본이 되는 미디어 자료를 저장합니다.")
    public MediaAssetResponse createMediaAsset(@Valid @RequestBody MediaAssetRequest request) {
        return platformService.createMediaAsset(request);
    }

    @GetMapping("/localization-jobs")
    @Operation(summary = "현지화 작업 조회", description = "Perso 더빙/자막 작업 상태를 조회합니다.")
    public LocalizationJobListResponse localizationJobs() {
        return new LocalizationJobListResponse(platformService.localizationJobs());
    }

    @PostMapping("/localization-jobs")
    @Operation(summary = "현지화 작업 요청 저장", description = "실제 Perso 호출 전, 요청과 상태를 서버 DB에 저장합니다.")
    public LocalizationJobResponse createLocalizationJob(@Valid @RequestBody LocalizationJobRequest request) {
        return platformService.createLocalizationJob(request);
    }

    @PostMapping("/localization-jobs/{jobId}/submit")
    @Operation(summary = "Perso 작업 제출", description = "저장된 미디어 자료를 Perso 번역/더빙 작업으로 제출하고 provider job id를 저장합니다.")
    public LocalizationJobResponse submitLocalizationJob(
        @PathVariable java.util.UUID jobId,
        @RequestBody(required = false) LocalizationJobSubmitRequest request
    ) {
        LocalizationJobSubmitRequest effectiveRequest = request == null
            ? new LocalizationJobSubmitRequest(null, null, null, null)
            : request;
        return persoLocalizationService.submit(effectiveRequest, jobId);
    }

    @PostMapping("/localization-jobs/{jobId}/sync")
    @Operation(summary = "Perso 작업 상태 동기화", description = "Perso 진행률과 산출물 링크를 확인해 서버 작업 상태를 갱신합니다.")
    public LocalizationJobResponse syncLocalizationJob(@PathVariable java.util.UUID jobId) {
        return persoLocalizationService.sync(jobId);
    }

    @PostMapping("/automation/webhooks/{source}")
    @Operation(summary = "자동화 웹훅 수신", description = "n8n 등 자동화 도구가 보낸 이벤트를 서버에 저장합니다.")
    public AutomationWebhookResponse automationWebhook(
        @PathVariable String source,
        @Valid @RequestBody AutomationWebhookRequest request
    ) {
        return platformService.storeAutomationEvent(source, request);
    }
}
