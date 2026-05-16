package com.alpaka.stock.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "research_snapshots")
public class ResearchSnapshot extends BaseEntity {
    @Column(length = 120)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(nullable = false, columnDefinition = "text")
    private String note;

    @Column(nullable = false, length = 24)
    private String stance;

    @Column(nullable = false, length = 24)
    private String conviction;

    @Column(nullable = false, columnDefinition = "text")
    private String thesis;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal price;

    @Column(nullable = false, precision = 12, scale = 6)
    private BigDecimal changePercent;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal score;

    @Column(length = 240)
    private String selectedEventTitle;

    @Column(length = 40)
    private String selectedEventDate;

    @Column(columnDefinition = "text")
    private String activeRuleLabels;

    @Column(length = 160)
    private String presetName;

    protected ResearchSnapshot() {
    }

    public ResearchSnapshot(
        String userId,
        Instrument instrument,
        String note,
        String stance,
        String conviction,
        String thesis,
        BigDecimal price,
        BigDecimal changePercent,
        BigDecimal score,
        String selectedEventTitle,
        String selectedEventDate,
        String activeRuleLabels,
        String presetName
    ) {
        this.userId = userId;
        this.instrument = instrument;
        this.note = note;
        this.stance = stance;
        this.conviction = conviction;
        this.thesis = thesis;
        this.price = price;
        this.changePercent = changePercent;
        this.score = score;
        this.selectedEventTitle = selectedEventTitle;
        this.selectedEventDate = selectedEventDate;
        this.activeRuleLabels = activeRuleLabels;
        this.presetName = presetName;
    }

    public String getUserId() {
        return userId;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public String getNote() {
        return note;
    }

    public String getStance() {
        return stance;
    }

    public String getConviction() {
        return conviction;
    }

    public String getThesis() {
        return thesis;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getChangePercent() {
        return changePercent;
    }

    public BigDecimal getScore() {
        return score;
    }

    public String getSelectedEventTitle() {
        return selectedEventTitle;
    }

    public String getSelectedEventDate() {
        return selectedEventDate;
    }

    public String getActiveRuleLabels() {
        return activeRuleLabels;
    }

    public String getPresetName() {
        return presetName;
    }
}
