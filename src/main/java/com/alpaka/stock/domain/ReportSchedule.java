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
}
