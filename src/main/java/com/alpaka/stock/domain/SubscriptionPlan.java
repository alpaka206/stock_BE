package com.alpaka.stock.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan extends BaseEntity {
    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyPrice;

    @Column(nullable = false, length = 12)
    private String currency;

    @Column(nullable = false, columnDefinition = "text")
    private String featureLimits;

    @Column(nullable = false)
    private boolean active = true;

    protected SubscriptionPlan() {
    }

    public SubscriptionPlan(
        String code,
        String name,
        BigDecimal monthlyPrice,
        String currency,
        String featureLimits,
        boolean active
    ) {
        this.code = code;
        this.name = name;
        this.monthlyPrice = monthlyPrice;
        this.currency = currency;
        this.featureLimits = featureLimits;
        this.active = active;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public String getFeatureLimits() {
        return featureLimits;
    }

    public boolean isActive() {
        return active;
    }
}
