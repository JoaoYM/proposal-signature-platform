package com.powermobile.sign.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powermobile.sign.domain.port.in.ContratoUseCase;
import com.powermobile.sign.domain.repository.ContratoRepository;
import com.powermobile.sign.domain.repository.InboxEventRepository;
import com.powermobile.sign.domain.model.InboxEvent;
import com.powermobile.sign.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PropostaCriadaListener {

    private final ContratoUseCase contratoUseCase;
    private final ContratoRepository contratoRepository;
    private final ObjectMapper objectMapper;
    private final InboxEventRepository inboxEventRepository;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PROPOSTA_CRIADA)
    @Transactional
    public void processarPropostaCriada(String payload) {
        try {
            log.info("Evento recebido do CRM: {}", payload);
            
            // Leitura do JSON que veio do Outbox do CRM
            JsonNode envelope = objectMapper.readTree(payload);
            if (!"PROPOSTA_CRIADA".equals(envelope.path("eventType").asText()) || envelope.path("version").asInt() != 1) {
                throw new IllegalArgumentException("Envelope de evento não suportado");
            }
            JsonNode jsonNode = envelope.path("data");
            UUID eventId = UUID.fromString(envelope.path("eventId").asText());
            if (inboxEventRepository.existsById(eventId)) { return; }
            String propostaId = jsonNode.get("id").asText();
            String clienteNome = jsonNode.get("clienteNome").asText();
            String clienteEmail = jsonNode.get("clienteEmail").asText();

            // IDEMPOTÊNCIA: Verifica se já existe um contrato para esta proposta no banco de dados.
            if (contratoRepository.findByPropostaId(propostaId).isPresent()) {
                log.warn("Contrato já existe para a proposta {}. Evento ignorado.", propostaId);
                return;
            }

            contratoUseCase.gerarContratoDaProposta(propostaId, clienteNome, clienteEmail);
            inboxEventRepository.save(InboxEvent.builder().eventId(eventId).eventType(envelope.path("eventType").asText())
                    .aggregateId(envelope.path("aggregateId").asText()).occurredAt(Instant.parse(envelope.path("occurredAt").asText()).atZone(ZoneOffset.UTC).toLocalDateTime())
                    .processedAt(LocalDateTime.now(ZoneOffset.UTC)).build());
            log.info("Contrato gerado com sucesso para a proposta {}", propostaId);

        } catch (Exception e) {
            log.error("Erro ao processar evento de proposta criada", e);
            // Ao lançar a exceção, o Spring AMQP sinaliza NACK para o RabbitMQ,
            // que pode recolocar a mensagem na fila ou enviá-la para uma Dead Letter Queue (DLQ).
            throw new RuntimeException("Falha no processamento da mensagem", e);
        }
    }
}
