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
}
