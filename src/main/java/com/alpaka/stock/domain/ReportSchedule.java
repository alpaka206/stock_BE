package com.alpaka.stock.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "report_schedules")
public class ReportSchedule extends BaseEntity {
    @Column(nullable = false, length = 120)
    private String userId;

    @Column(nullable = false, length = 16)
    private String locale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReportCadence cadence;

    @Column(nullable = false, length = 240)
    private String deliveryEmail;

    @Column(nullable = false, length = 80)
    private String timezone;

    @Column(nullable = false)
    private boolean enabled = true;

    protected ReportSchedule() {
    }

    public ReportSchedule(
        String userId,
        String locale,
        ReportCadence cadence,
        String deliveryEmail,
        String timezone,
        boolean enabled
    ) {
        this.userId = userId;
        this.locale = locale;
        this.cadence = cadence;
        this.deliveryEmail = deliveryEmail;
        this.timezone = timezone;
        this.enabled = enabled;
    }

    public void update(String locale, ReportCadence cadence, String deliveryEmail, String timezone, boolean enabled) {
        this.locale = locale;
        this.cadence = cadence;
        this.deliveryEmail = deliveryEmail;
        this.timezone = timezone;
        this.enabled = enabled;
    }

    public String getUserId() {
        return userId;
    }

    public String getLocale() {
        return locale;
    }

    public ReportCadence getCadence() {
        return cadence;
    }

    public String getDeliveryEmail() {
        return deliveryEmail;
    }

    public String getTimezone() {
        return timezone;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
