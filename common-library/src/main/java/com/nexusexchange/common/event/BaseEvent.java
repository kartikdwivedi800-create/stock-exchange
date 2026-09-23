package com.nexusexchange.common.event;

import com.nexusexchange.common.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {
    private String eventId;
    private EventType eventType;
    private Instant occurredAt;
    private String correlationId;
    private String version;
}
