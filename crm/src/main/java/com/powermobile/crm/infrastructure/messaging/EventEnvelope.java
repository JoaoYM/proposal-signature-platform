package com.powermobile.crm.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public record EventEnvelope(UUID eventId, String eventType, String occurredAt, int version,
                            String correlationId, String aggregateId, JsonNode data) {}
