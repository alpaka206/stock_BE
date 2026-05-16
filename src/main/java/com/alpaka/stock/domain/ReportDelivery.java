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
@Table(name = "report_deliveries")
public class ReportDelivery extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_schedule_id")
    private ReportSchedule reportSchedule;

    @Column(nullable = false, length = 120)
    private String userId;

    @Column(nullable = false, length = 16)
    private String locale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReportCadence cadence;

    @Column(nullable = false, length = 240)
    private String deliveryEmail;

    @Column(nullable = false, length = 240)
    private String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String textBody;

    @Column(nullable = false, columnDefinition = "text")
    private String htmlBody;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ReportDeliveryStatus status;

    @Column(columnDefinition = "text")
    private String errorMessage;

    @Column(nullable = false)
    private OffsetDateTime generatedAt;

    private OffsetDateTime sentAt;

    protected ReportDelivery() {
    }

    public ReportDelivery(
        ReportSchedule reportSchedule,
        String userId,
        String locale,
        ReportCadence cadence,
        String deliveryEmail,
        String subject,
        String textBody,
        String htmlBody
    ) {
        this.reportSchedule = reportSchedule;
        this.userId = userId;
        this.locale = locale;
        this.cadence = cadence;
        this.deliveryEmail = deliveryEmail;
        this.subject = subject;
        this.textBody = textBody;
        this.htmlBody = htmlBody;
        this.status = ReportDeliveryStatus.READY;
        this.generatedAt = OffsetDateTime.now();
    }

    public void markSent() {
        this.status = ReportDeliveryStatus.SENT;
        this.sentAt = OffsetDateTime.now();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = ReportDeliveryStatus.FAILED;
        this.errorMessage = errorMessage;
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

    public String getSubject() {
        return subject;
    }

    public String getTextBody() {
        return textBody;
    }

    public String getHtmlBody() {
        return htmlBody;
    }

    public ReportDeliveryStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public OffsetDateTime getGeneratedAt() {
        return generatedAt;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }
}
