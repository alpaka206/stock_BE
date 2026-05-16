package com.alpaka.stock.service;

import com.alpaka.stock.api.dto.PlatformDtos.LocalizationJobResponse;
import com.alpaka.stock.api.dto.PlatformDtos.LocalizationJobSubmitRequest;
import com.alpaka.stock.domain.LocalizationJob;
import com.alpaka.stock.domain.MediaAsset;
import com.alpaka.stock.domain.MediaKind;
import com.alpaka.stock.repository.LocalizationJobRepository;
import com.alpaka.stock.repository.MediaAssetRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityNotFoundException;
import java.net.URI;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class PersoLocalizationService {
    private final LocalizationJobRepository localizationJobRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String baseUrl;
    private final String portalMediaBaseUrl;
    private final String apiKey;
    private final Long spaceSeq;

    public PersoLocalizationService(
        LocalizationJobRepository localizationJobRepository,
        MediaAssetRepository mediaAssetRepository,
        ObjectMapper objectMapper,
        RestClient.Builder restClientBuilder,
        @Value("${stock.providers.perso.base-url}") String baseUrl,
        @Value("${stock.providers.perso.portal-media-base-url:https://portal-media.perso.ai}") String portalMediaBaseUrl,
        @Value("${stock.providers.perso.api-key}") String apiKey,
        @Value("${stock.providers.perso.space-seq:0}") Long spaceSeq
    ) {
        this.localizationJobRepository = localizationJobRepository;
        this.mediaAssetRepository = mediaAssetRepository;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
        this.baseUrl = baseUrl;
        this.portalMediaBaseUrl = portalMediaBaseUrl;
        this.apiKey = apiKey;
        this.spaceSeq = spaceSeq;
    }

    @Transactional
    public LocalizationJobResponse submit(LocalizationJobSubmitRequest request, java.util.UUID jobId) {
        requirePersoConfig();
        LocalizationJob job = localizationJobRepository.findById(jobId)
            .orElseThrow(() -> new EntityNotFoundException("현지화 작업을 찾을 수 없습니다: " + jobId));
        MediaAsset mediaAsset = job.getMediaAsset();
        long mediaSeq = registerMedia(mediaAsset);
        initializeQueue();
        JsonNode translationPayload = requestTranslation(mediaAsset, job, request, mediaSeq);
        String providerJobId = extractProjectId(translationPayload);
        job.markProcessing(providerJobId, translationPayload.toString());
        return toResponse(localizationJobRepository.save(job));
    }

    @Transactional
    public LocalizationJobResponse sync(java.util.UUID jobId) {
        requirePersoConfig();
        LocalizationJob job = localizationJobRepository.findById(jobId)
            .orElseThrow(() -> new EntityNotFoundException("현지화 작업을 찾을 수 없습니다: " + jobId));
        if (job.getProviderJobId() == null || job.getProviderJobId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Perso provider job id가 아직 없습니다.");
        }

        JsonNode progress = getJson("/video-translator/api/v1/projects/%s/space/%s/progress".formatted(
            job.getProviderJobId(),
            spaceSeq
        ));
        String reason = progress.path("result").path("progressReason").asText("");
        boolean hasFailed = progress.path("result").path("hasFailed").asBoolean(false)
            || "Failed".equalsIgnoreCase(reason);
        if (hasFailed) {
            job.markFailed("Perso 작업이 실패했습니다: " + reason, progress.toString());
            return toResponse(localizationJobRepository.save(job));
        }
        if ("Completed".equalsIgnoreCase(reason)) {
            JsonNode download = getJson("/video-translator/api/v1/projects/%s/spaces/%s/download?target=all".formatted(
                job.getProviderJobId(),
                spaceSeq
            ));
            job.markCompleted(
                download.path("result").path("audioFile").path("voiceAudioDownloadLink").asText(null),
                download.path("result").path("srtFile").path("translatedSubtitleDownloadLink").asText(null),
                download.toString()
            );
            return toResponse(localizationJobRepository.save(job));
        }

        job.markProcessing(job.getProviderJobId(), progress.toString());
        return toResponse(localizationJobRepository.save(job));
    }

    private long registerMedia(MediaAsset mediaAsset) {
        if (mediaAsset.getKind() == MediaKind.VIDEO) {
            JsonNode externalPayload = putJson(
                "/file/api/upload/video/external",
                objectMapper.createObjectNode()
                    .put("space_seq", spaceSeq)
                    .put("url", mediaAsset.getSourceUrl())
                    .put("lang", mediaAsset.getLanguage())
            );
            return externalPayload.path("seq").asLong();
        }

        JsonNode audioPayload = putJson(
            "/file/api/upload/audio",
            objectMapper.createObjectNode()
                .put("spaceSeq", spaceSeq)
                .put("fileUrl", mediaAsset.getSourceUrl())
                .put("fileName", safeFileName(mediaAsset.getTitle(), ".mp3"))
        );
        return audioPayload.path("seq").asLong();
    }

    private void initializeQueue() {
        try {
            putJson("/video-translator/api/v1/projects/spaces/%s/queue".formatted(spaceSeq), objectMapper.createObjectNode());
        } catch (RuntimeException ignored) {
            // 이미 큐가 있거나 plan별 정책으로 실패할 수 있어 번역 요청에서 최종 오류를 확인합니다.
        }
    }

    private JsonNode requestTranslation(
        MediaAsset mediaAsset,
        LocalizationJob job,
        LocalizationJobSubmitRequest request,
        long mediaSeq
    ) {
        ObjectNode body = objectMapper.createObjectNode()
            .put("mediaSeq", mediaSeq)
            .put("isVideoProject", mediaAsset.getKind() == MediaKind.VIDEO)
            .put("sourceLanguageCode", request.sourceLanguageCodeOrDefault())
            .put("numberOfSpeakers", request.numberOfSpeakersOrDefault())
            .put("preferredSpeedType", request.preferredSpeedTypeOrDefault())
            .put("withLipSync", request.withLipSyncOrDefault())
            .put("title", mediaAsset.getTitle());
        body.putArray("targetLanguageCodes").add(job.getTargetLanguage());

        return postJson(
            "/video-translator/api/v1/projects/spaces/%s/translate".formatted(spaceSeq),
            body
        );
    }

    private String extractProjectId(JsonNode payload) {
        JsonNode ids = payload.path("result").path("startGenerateProjectIdList");
        if (!ids.isArray() || ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Perso 번역 요청 응답에 project id가 없습니다.");
        }
        return ids.get(0).asText();
    }

    private JsonNode getJson(String path) {
        JsonNode response = restClient.get()
            .uri(resolve(path))
            .header("XP-API-KEY", apiKey)
            .retrieve()
            .body(JsonNode.class);
        return requireResponse(response);
    }

    private JsonNode postJson(String path, ObjectNode body) {
        JsonNode response = restClient.post()
            .uri(resolve(path))
            .header("XP-API-KEY", apiKey)
            .body(body)
            .retrieve()
            .body(JsonNode.class);
        return requireResponse(response);
    }

    private JsonNode putJson(String path, ObjectNode body) {
        JsonNode response = restClient.put()
            .uri(resolve(path))
            .header("XP-API-KEY", apiKey)
            .body(body)
            .retrieve()
            .body(JsonNode.class);
        return requireResponse(response);
    }

    private JsonNode requireResponse(JsonNode response) {
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Perso 응답이 비어 있습니다.");
        }
        return response;
    }

    private URI resolve(String path) {
        return UriComponentsBuilder.fromUriString(baseUrl.replaceAll("/$", "") + "/" + path.replaceAll("^/", ""))
            .build(true)
            .toUri();
    }

    private void requirePersoConfig() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(
                HttpStatus.PRECONDITION_REQUIRED,
                "PERSO_API_KEY가 설정되지 않았습니다. Perso API 키는 서버 환경변수로만 관리해야 합니다."
            );
        }
        if (spaceSeq == null || spaceSeq <= 0) {
            throw new ResponseStatusException(
                HttpStatus.PRECONDITION_REQUIRED,
                "PERSO_SPACE_SEQ가 설정되지 않았습니다. Perso Space ID를 서버 환경변수로 넣어 주세요."
            );
        }
    }

    private String safeFileName(String title, String extension) {
        String normalized = title == null || title.isBlank() ? "stock-media" : title.trim();
        String cleaned = normalized.replaceAll("[^A-Za-z0-9._-]", "-");
        if (!cleaned.endsWith(extension)) {
            cleaned += extension;
        }
        return cleaned;
    }

    private LocalizationJobResponse toResponse(LocalizationJob job) {
        String audioUrl = resolvePortalUrl(job.getDubbedAudioUrl());
        String subtitleUrl = resolvePortalUrl(job.getSubtitleUrl());
        return new LocalizationJobResponse(
            job.getId(),
            job.getMediaAsset().getId(),
            job.getProvider(),
            job.getProviderJobId(),
            job.getTargetLanguage(),
            job.getStatus(),
            audioUrl,
            subtitleUrl,
            job.getErrorMessage(),
            job.getRequestedAt(),
            job.getCompletedAt()
        );
    }

    private String resolvePortalUrl(String value) {
        if (value == null || value.isBlank() || value.startsWith("http")) {
            return value;
        }
        return portalMediaBaseUrl.replaceAll("/$", "") + "/" + value.replaceAll("^/", "");
    }
}
