package com.nexusexchange.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Mirrors table {@code outbox} (migration V2). Written in the same transaction as the domain change; a poller publishes to Kafka. */
@Entity
@Table(name = "outbox")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    private UUID id;

    /** The kind of aggregate that was changed (e.g. "Order"). */
    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    /** The primary key of the changed aggregate, as a string. */
    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    /** Logical name of the domain event (e.g. "OrderPlaced"). */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /** Kafka topic to publish to. */
    @Column(nullable = false, length = 100)
    private String topic;

    /** Kafka partition key; typically the userId or orderId. */
    @Column(name = "message_key", length = 100)
    private String messageKey;

    /** Full event payload as JSON; stored as JSONB for query efficiency. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Set by the outbox poller after the message is successfully acknowledged by Kafka. */
    @Column(name = "published_at")
    private Instant publishedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        }
    }
}
