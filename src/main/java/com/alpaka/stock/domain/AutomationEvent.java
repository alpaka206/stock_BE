package com.alpaka.stock.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "automation_events")
public class AutomationEvent extends BaseEntity {
    @Column(nullable = false, length = 80)
    private String source;

    @Column(nullable = false, length = 120)
    private String eventType;

    @Column(nullable = false, length = 24)
    private String status;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    protected AutomationEvent() {
    }

    public AutomationEvent(String source, String eventType, String status, String payload) {
        this.source = source;
        this.eventType = eventType;
        this.status = status;
        this.payload = payload;
    }

    public String getSource() {
        return source;
    }

    public String getEventType() {
        return eventType;
    }

    public String getStatus() {
        return status;
    }

    public String getPayload() {
        return payload;
    }
}
