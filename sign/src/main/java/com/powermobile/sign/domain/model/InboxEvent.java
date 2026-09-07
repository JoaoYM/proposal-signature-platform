package com.powermobile.sign.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity @Table(name = "inbox_events") @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED) @AllArgsConstructor @Builder
public class InboxEvent {
    @Id @Column(name = "event_id") private UUID eventId;
    @Column(name = "event_type", nullable = false) private String eventType;
    @Column(name = "aggregate_id", nullable = false) private String aggregateId;
    @Column(name = "occurred_at", nullable = false) private LocalDateTime occurredAt;
    @Column(name = "processed_at", nullable = false) private LocalDateTime processedAt;
}
