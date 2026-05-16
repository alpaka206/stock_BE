package com.alpaka.stock.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "localization_jobs")
public class LocalizationJob extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_asset_id", nullable = false)
    private MediaAsset mediaAsset;

    @Column(nullable = false, length = 80)
    private String provider;

    @Column(nullable = false, length = 16)
    private String targetLanguage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private LocalizationStatus status;

    @Column(length = 120)
    private String providerJobId;

    @Column(length = 1000)
    private String dubbedAudioUrl;

    @Column(length = 1000)
    private String subtitleUrl;

    @Column(columnDefinition = "text")
    private String errorMessage;

    @Column(columnDefinition = "text")
    private String providerPayload;

    @Column(nullable = false)
    private OffsetDateTime requestedAt;

    private OffsetDateTime completedAt;

    protected LocalizationJob() {
    }

    public LocalizationJob(MediaAsset mediaAsset, String provider, String targetLanguage) {
        this.mediaAsset = mediaAsset;
        this.provider = provider;
        this.targetLanguage = targetLanguage;
        this.status = LocalizationStatus.REQUESTED;
        this.requestedAt = OffsetDateTime.now();
    }

    public MediaAsset getMediaAsset() {
        return mediaAsset;
    }

    public String getProvider() {
        return provider;
    }

    public String getTargetLanguage() {
        return targetLanguage;
    }

    public LocalizationStatus getStatus() {
        return status;
    }

    public String getProviderJobId() {
        return providerJobId;
    }

    public String getDubbedAudioUrl() {
        return dubbedAudioUrl;
    }

    public String getSubtitleUrl() {
        return subtitleUrl;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public OffsetDateTime getRequestedAt() {
        return requestedAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void markProcessing(String providerJobId, String providerPayload) {
        this.status = LocalizationStatus.PROCESSING;
        this.providerJobId = providerJobId;
        this.providerPayload = providerPayload;
        this.errorMessage = null;
    }

    public void markCompleted(String dubbedAudioUrl, String subtitleUrl, String providerPayload) {
        this.status = LocalizationStatus.COMPLETED;
        this.dubbedAudioUrl = dubbedAudioUrl;
        this.subtitleUrl = subtitleUrl;
        this.providerPayload = providerPayload;
        this.completedAt = OffsetDateTime.now();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage, String providerPayload) {
        this.status = LocalizationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.providerPayload = providerPayload;
    }
}
