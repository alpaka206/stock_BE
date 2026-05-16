package com.alpaka.stock.service;

import com.alpaka.stock.api.dto.PlatformDtos.AutomationWebhookRequest;
import com.alpaka.stock.api.dto.PlatformDtos.AutomationWebhookResponse;
import com.alpaka.stock.api.dto.PlatformDtos.LocalizationJobRequest;
import com.alpaka.stock.api.dto.PlatformDtos.LocalizationJobResponse;
import com.alpaka.stock.api.dto.PlatformDtos.MediaAssetRequest;
import com.alpaka.stock.api.dto.PlatformDtos.MediaAssetResponse;
import com.alpaka.stock.api.dto.PlatformDtos.ReportScheduleRequest;
import com.alpaka.stock.api.dto.PlatformDtos.ReportScheduleResponse;
import com.alpaka.stock.api.dto.PlatformDtos.SubscriptionPlanResponse;
import com.alpaka.stock.domain.AutomationEvent;
import com.alpaka.stock.domain.Instrument;
import com.alpaka.stock.domain.LocalizationJob;
import com.alpaka.stock.domain.MediaAsset;
import com.alpaka.stock.domain.ReportSchedule;
import com.alpaka.stock.domain.SourceMaterial;
import com.alpaka.stock.domain.SubscriptionPlan;
import com.alpaka.stock.repository.AutomationEventRepository;
import com.alpaka.stock.repository.InstrumentRepository;
import com.alpaka.stock.repository.LocalizationJobRepository;
import com.alpaka.stock.repository.MediaAssetRepository;
import com.alpaka.stock.repository.ReportScheduleRepository;
import com.alpaka.stock.repository.SourceMaterialRepository;
import com.alpaka.stock.repository.SubscriptionPlanRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformService {
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final ReportScheduleRepository reportScheduleRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final LocalizationJobRepository localizationJobRepository;
    private final AutomationEventRepository automationEventRepository;
    private final InstrumentRepository instrumentRepository;
    private final SourceMaterialRepository sourceMaterialRepository;
    private final ObjectMapper objectMapper;

    public PlatformService(
        SubscriptionPlanRepository subscriptionPlanRepository,
        ReportScheduleRepository reportScheduleRepository,
        MediaAssetRepository mediaAssetRepository,
        LocalizationJobRepository localizationJobRepository,
        AutomationEventRepository automationEventRepository,
        InstrumentRepository instrumentRepository,
        SourceMaterialRepository sourceMaterialRepository,
        ObjectMapper objectMapper
    ) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.reportScheduleRepository = reportScheduleRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.localizationJobRepository = localizationJobRepository;
        this.automationEventRepository = automationEventRepository;
        this.instrumentRepository = instrumentRepository;
        this.sourceMaterialRepository = sourceMaterialRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<SubscriptionPlanResponse> subscriptionPlans() {
        ensureDefaultPlans();
        return subscriptionPlanRepository.findByActiveTrueOrderByMonthlyPriceAsc()
            .stream()
            .map(this::toPlanResponse)
            .toList();
    }

    @Transactional
    public ReportScheduleResponse createReportSchedule(ReportScheduleRequest request) {
        ReportSchedule schedule = new ReportSchedule(
            request.userId(),
            request.locale(),
            request.cadence(),
            request.deliveryEmail(),
            request.timezone(),
            request.enabledOrDefault()
        );
        return toScheduleResponse(reportScheduleRepository.save(schedule));
    }

    @Transactional(readOnly = true)
    public List<ReportScheduleResponse> reportSchedules(String userId) {
        return reportScheduleRepository.findTop50ByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(this::toScheduleResponse)
            .toList();
    }

    @Transactional
    public MediaAssetResponse createMediaAsset(MediaAssetRequest request) {
        Instrument instrument = request.symbol() == null || request.symbol().isBlank()
            ? null
            : instrumentRepository.findFirstBySymbolIgnoreCase(request.symbol()).orElse(null);
        SourceMaterial material = request.materialId() == null
            ? null
            : sourceMaterialRepository.findById(request.materialId()).orElse(null);
        MediaAsset mediaAsset = new MediaAsset(
            instrument,
            material,
            request.kind(),
            request.title(),
            request.sourceUrl(),
            request.provider(),
            request.language(),
            request.publishedAt()
        );
        return toMediaResponse(mediaAssetRepository.save(mediaAsset));
    }

    @Transactional(readOnly = true)
    public List<MediaAssetResponse> mediaAssets() {
        return mediaAssetRepository.findTop50ByOrderByPublishedAtDesc()
            .stream()
            .map(this::toMediaResponse)
            .toList();
    }

    @Transactional
    public LocalizationJobResponse createLocalizationJob(LocalizationJobRequest request) {
        MediaAsset mediaAsset = mediaAssetRepository.findById(request.mediaAssetId())
            .orElseThrow(() -> new EntityNotFoundException("미디어 자료를 찾을 수 없습니다: " + request.mediaAssetId()));
        LocalizationJob job = new LocalizationJob(
            mediaAsset,
            request.provider(),
            request.targetLanguage()
        );
        return toLocalizationResponse(localizationJobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public List<LocalizationJobResponse> localizationJobs() {
        return localizationJobRepository.findTop50ByOrderByRequestedAtDesc()
            .stream()
            .map(this::toLocalizationResponse)
            .toList();
    }

    @Transactional
    public AutomationWebhookResponse storeAutomationEvent(String source, AutomationWebhookRequest request) {
        AutomationEvent event = new AutomationEvent(
            source,
            request.eventType(),
            "received",
            toJson(request.payload())
        );
        AutomationEvent saved = automationEventRepository.save(event);
        return new AutomationWebhookResponse(
            saved.getId(),
            saved.getSource(),
            saved.getEventType(),
            saved.getStatus(),
            saved.getCreatedAt()
        );
    }

    private void ensureDefaultPlans() {
        createPlanIfAbsent(
            "free",
            "Free",
            BigDecimal.ZERO,
            "KRW",
            """
            {"watchlistSymbols":10,"savedSnapshots":20,"dailyReports":false,"weeklyReports":true,"mediaLocalizationMinutes":0}
            """
        );
        createPlanIfAbsent(
            "pro",
            "Pro",
            BigDecimal.valueOf(19000),
            "KRW",
            """
            {"watchlistSymbols":100,"savedSnapshots":500,"dailyReports":true,"weeklyReports":true,"mediaLocalizationMinutes":60}
            """
        );
        createPlanIfAbsent(
            "team",
            "Team",
            BigDecimal.valueOf(59000),
            "KRW",
            """
            {"watchlistSymbols":500,"savedSnapshots":5000,"dailyReports":true,"weeklyReports":true,"mediaLocalizationMinutes":300,"sharedWorkspace":true}
            """
        );
    }

    private void createPlanIfAbsent(
        String code,
        String name,
        BigDecimal monthlyPrice,
        String currency,
        String featureLimits
    ) {
        if (subscriptionPlanRepository.findByCodeIgnoreCase(code).isPresent()) {
            return;
        }
        subscriptionPlanRepository.save(new SubscriptionPlan(
            code,
            name,
            monthlyPrice,
            currency,
            featureLimits.trim(),
            true
        ));
    }

    private SubscriptionPlanResponse toPlanResponse(SubscriptionPlan plan) {
        return new SubscriptionPlanResponse(
            plan.getId(),
            plan.getCode(),
            plan.getName(),
            plan.getMonthlyPrice(),
            plan.getCurrency(),
            plan.getFeatureLimits(),
            plan.isActive()
        );
    }

    private ReportScheduleResponse toScheduleResponse(ReportSchedule schedule) {
        return new ReportScheduleResponse(
            schedule.getId(),
            schedule.getUserId(),
            schedule.getLocale(),
            schedule.getCadence(),
            schedule.getDeliveryEmail(),
            schedule.getTimezone(),
            schedule.isEnabled(),
            schedule.getCreatedAt()
        );
    }

    private MediaAssetResponse toMediaResponse(MediaAsset mediaAsset) {
        return new MediaAssetResponse(
            mediaAsset.getId(),
            mediaAsset.getInstrument() == null ? "" : mediaAsset.getInstrument().getSymbol(),
            mediaAsset.getMaterial() == null ? null : mediaAsset.getMaterial().getId(),
            mediaAsset.getKind(),
            mediaAsset.getTitle(),
            mediaAsset.getSourceUrl(),
            mediaAsset.getProvider(),
            mediaAsset.getLanguage(),
            mediaAsset.getPublishedAt(),
            mediaAsset.getCreatedAt()
        );
    }

    private LocalizationJobResponse toLocalizationResponse(LocalizationJob job) {
        return new LocalizationJobResponse(
            job.getId(),
            job.getMediaAsset().getId(),
            job.getProvider(),
            job.getTargetLanguage(),
            job.getStatus(),
            job.getDubbedAudioUrl(),
            job.getSubtitleUrl(),
            job.getErrorMessage(),
            job.getRequestedAt(),
            job.getCompletedAt()
        );
    }

    private String toJson(Object value) {
        if (value == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exc) {
            throw new IllegalArgumentException("자동화 payload를 JSON으로 저장할 수 없습니다.", exc);
        }
    }
}
