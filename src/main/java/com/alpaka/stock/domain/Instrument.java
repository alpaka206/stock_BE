package com.alpaka.stock.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "instruments")
public class Instrument extends BaseEntity {
    @Column(nullable = false, length = 32)
    private String symbol;

    @Column(nullable = false, length = 160)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Market market;

    @Column(nullable = false, length = 40)
    private String exchange;

    @Column(nullable = false, length = 40)
    private String securityCode;

    @Column(length = 80)
    private String sector;

    @Column(nullable = false, length = 12)
    private String currency;

    @Column(nullable = false)
    private boolean active = true;

    protected Instrument() {
    }

    public Instrument(
        String symbol,
        String name,
        Market market,
        String exchange,
        String securityCode,
        String sector,
        String currency
    ) {
        this.symbol = symbol.toUpperCase();
        this.name = name;
        this.market = market;
        this.exchange = exchange;
        this.securityCode = securityCode;
        this.sector = sector;
        this.currency = currency;
    }

    public void updateProfile(String name, String sector, String currency, boolean active) {
        this.name = name;
        this.sector = sector;
        this.currency = currency;
        this.active = active;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getName() {
        return name;
    }

    public Market getMarket() {
        return market;
    }

    public String getExchange() {
        return exchange;
    }

    public String getSecurityCode() {
        return securityCode;
    }

    public String getSector() {
        return sector;
    }

    public String getCurrency() {
        return currency;
    }

    public boolean isActive() {
        return active;
    }
}
