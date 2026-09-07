package com.powermobile.sign.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powermobile.sign.domain.exception.AssinaturaDomainException;
import com.powermobile.sign.domain.model.OutboxEvent;
import com.powermobile.sign.domain.port.out.EventPublisher;
import com.powermobile.sign.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.powermobile.sign.infrastructure.messaging.EventEnvelope;
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
            String eventType = "CONTRATO_STATUS".equals(aggregateType) ? "CONTRATO_STATUS_ALTERADO" : aggregateType;
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
            throw new AssinaturaDomainException("Falha na integridade do evento", e);
        }
    }
}
