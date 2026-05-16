package com.alpaka.stock.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "price_bars")
public class PriceBar extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(nullable = false)
    private LocalDate tradeDate;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal openPrice;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal highPrice;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal lowPrice;

    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal closePrice;

    @Column(nullable = false, precision = 24, scale = 0)
    private BigDecimal volume;

    @Column(nullable = false, length = 80)
    private String provider;

    @Column(nullable = false, length = 160)
    private String sourceKey;

    protected PriceBar() {
    }

    public PriceBar(
        Instrument instrument,
        LocalDate tradeDate,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        BigDecimal volume,
        String provider,
        String sourceKey
    ) {
        this.instrument = instrument;
        this.tradeDate = tradeDate;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.provider = provider;
        this.sourceKey = sourceKey;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public BigDecimal getClosePrice() {
        return closePrice;
    }

    public BigDecimal getOpenPrice() {
        return openPrice;
    }

    public BigDecimal getHighPrice() {
        return highPrice;
    }

    public BigDecimal getLowPrice() {
        return lowPrice;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public String getProvider() {
        return provider;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public void updatePrices(
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        BigDecimal volume,
        String sourceKey
    ) {
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.sourceKey = sourceKey;
    }
}
