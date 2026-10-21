package com.nexusexchange.wallet.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** Mirrors table {@code processed_events} (migration V2). Guards Kafka consumers against duplicate delivery. */
@Entity
@Table(name = "processed_events")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    /** The eventId from the incoming Kafka message header / event envelope. */
    @Id
    @Column(name = "event_id")
    private UUID eventId;

    /** Logical name of the consumer bean that processed this event (e.g. "walletConsumer"). */
    @Column(nullable = false, length = 50)
    private String consumer;

    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    @PrePersist
    protected void onCreate() {
        if (processedAt == null) {
            processedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        }
    }
}
