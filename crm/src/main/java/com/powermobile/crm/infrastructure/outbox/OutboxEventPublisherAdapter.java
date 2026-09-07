package com.powermobile.crm.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powermobile.crm.domain.exception.PropostaDomainException;
import com.powermobile.crm.domain.model.OutboxEvent;
import com.powermobile.crm.domain.port.out.EventPublisher;
import com.powermobile.crm.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.powermobile.crm.infrastructure.messaging.EventEnvelope;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventPublisherAdapter implements EventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(String aggregateType, String aggregateId, Object payload) {
        try {
            UUID eventId = UUID.randomUUID();
            String eventType = "PROPOSTA".equals(aggregateType) ? "PROPOSTA_CRIADA" : aggregateType;
            EventEnvelope envelope = new EventEnvelope(eventId, eventType, Instant.now().toString(), 1,
                    aggregateId, aggregateId, objectMapper.valueToTree(payload));
            String json = objectMapper.writeValueAsString(envelope);
            OutboxEvent evento = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .payload(json)
                    .status("PENDING")
                    .build();
            outboxEventRepository.save(evento);
            log.info("Evento PENDING registrado no Outbox. Tipo: {}, AggregateId: {}", aggregateType, aggregateId);
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar payload do evento para o Outbox", e);
            throw new PropostaDomainException("Falha na integridade do evento", e);
        }
    }
}
