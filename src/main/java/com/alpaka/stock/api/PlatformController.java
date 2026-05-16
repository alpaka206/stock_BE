package com.alpaka.stock.api;

import com.alpaka.stock.api.dto.PlatformDtos.AutomationWebhookRequest;
import com.alpaka.stock.api.dto.PlatformDtos.AutomationWebhookResponse;
import com.alpaka.stock.api.dto.PlatformDtos.LocalizationJobListResponse;
import com.alpaka.stock.api.dto.PlatformDtos.LocalizationJobRequest;
import com.alpaka.stock.api.dto.PlatformDtos.LocalizationJobResponse;
import com.alpaka.stock.api.dto.PlatformDtos.MediaAssetListResponse;
import com.alpaka.stock.api.dto.PlatformDtos.MediaAssetRequest;
import com.alpaka.stock.api.dto.PlatformDtos.MediaAssetResponse;
import com.alpaka.stock.api.dto.PlatformDtos.ReportScheduleListResponse;
import com.alpaka.stock.api.dto.PlatformDtos.ReportScheduleRequest;
import com.alpaka.stock.api.dto.PlatformDtos.ReportScheduleResponse;
import com.alpaka.stock.api.dto.PlatformDtos.SubscriptionPlanListResponse;
import com.alpaka.stock.service.PlatformService;
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
public class PlatformController {
    private final PlatformService platformService;

    public PlatformController(PlatformService platformService) {
        this.platformService = platformService;
    }

    @GetMapping("/subscription-plans")
    public SubscriptionPlanListResponse subscriptionPlans() {
        return new SubscriptionPlanListResponse(platformService.subscriptionPlans());
    }

    @GetMapping("/report-schedules")
    public ReportScheduleListResponse reportSchedules(@RequestParam String userId) {
        return new ReportScheduleListResponse(platformService.reportSchedules(userId));
    }

    @PostMapping("/report-schedules")
    public ReportScheduleResponse createReportSchedule(@Valid @RequestBody ReportScheduleRequest request) {
        return platformService.createReportSchedule(request);
    }

    @GetMapping("/media-assets")
    public MediaAssetListResponse mediaAssets() {
        return new MediaAssetListResponse(platformService.mediaAssets());
    }

    @PostMapping("/media-assets")
    public MediaAssetResponse createMediaAsset(@Valid @RequestBody MediaAssetRequest request) {
        return platformService.createMediaAsset(request);
    }

    @GetMapping("/localization-jobs")
    public LocalizationJobListResponse localizationJobs() {
        return new LocalizationJobListResponse(platformService.localizationJobs());
    }

    @PostMapping("/localization-jobs")
    public LocalizationJobResponse createLocalizationJob(@Valid @RequestBody LocalizationJobRequest request) {
        return platformService.createLocalizationJob(request);
    }

    @PostMapping("/automation/webhooks/{source}")
    public AutomationWebhookResponse automationWebhook(
        @PathVariable String source,
        @Valid @RequestBody AutomationWebhookRequest request
    ) {
        return platformService.storeAutomationEvent(source, request);
    }
}
