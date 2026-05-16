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
@Table(name = "source_materials")
public class SourceMaterial extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id")
    private Instrument instrument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SourceKind kind;

    @Column(nullable = false, length = 80)
    private String provider;

    @Column(nullable = false, length = 120)
    private String publisher;

    @Column(nullable = false, length = 360)
    private String title;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(length = 1000)
    private String sourceUrl;

    @Column(nullable = false, length = 240)
    private String sourceKey;

    @Column(nullable = false, length = 16)
    private String language;

    private OffsetDateTime publishedAt;

    @Column(nullable = false)
    private OffsetDateTime fetchedAt;

    @Column(columnDefinition = "text")
    private String rawPayload;

    @Column(nullable = false, length = 96)
    private String checksum;

    protected SourceMaterial() {
    }

    public SourceMaterial(
        Instrument instrument,
        SourceKind kind,
        String provider,
        String publisher,
        String title,
        String summary,
        String sourceUrl,
        String sourceKey,
        String language,
        OffsetDateTime publishedAt,
        String rawPayload,
        String checksum
    ) {
        this.instrument = instrument;
        this.kind = kind;
        this.provider = provider;
        this.publisher = publisher;
        this.title = title;
        this.summary = summary;
        this.sourceUrl = sourceUrl;
        this.sourceKey = sourceKey;
        this.language = language;
        this.publishedAt = publishedAt;
        this.fetchedAt = OffsetDateTime.now();
        this.rawPayload = rawPayload;
        this.checksum = checksum;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public SourceKind getKind() {
        return kind;
    }

    public String getProvider() {
        return provider;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public String getLanguage() {
        return language;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }
}
