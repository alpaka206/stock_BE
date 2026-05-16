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

    @Column(length = 1000)
    private String dubbedAudioUrl;

    @Column(length = 1000)
    private String subtitleUrl;

    @Column(columnDefinition = "text")
    private String errorMessage;

    @Column(nullable = false)
    private OffsetDateTime requestedAt;

    private OffsetDateTime completedAt;

    protected LocalizationJob() {
    }
}
