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
@Table(name = "media_assets")
public class MediaAsset extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id")
    private Instrument instrument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private SourceMaterial material;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MediaKind kind;

    @Column(nullable = false, length = 240)
    private String title;

    @Column(nullable = false, length = 1000)
    private String sourceUrl;

    @Column(nullable = false, length = 80)
    private String provider;

    @Column(nullable = false, length = 16)
    private String language;

    private OffsetDateTime publishedAt;

    protected MediaAsset() {
    }
}
