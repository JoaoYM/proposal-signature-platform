package com.powermobile.crm.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powermobile.crm.domain.enums.PropostaStatus;
import com.powermobile.crm.domain.model.Proposta;
import com.powermobile.crm.domain.repository.PropostaRepository;
import com.powermobile.crm.domain.repository.InboxEventRepository;
import com.powermobile.crm.domain.model.InboxEvent;
import com.powermobile.crm.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.*;
import org.springframework.cache.CacheManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContratoStatusListener {

    private final PropostaRepository propostaRepository;
    private final ObjectMapper objectMapper;
    private final InboxEventRepository inboxEventRepository;
    private final CacheManager cacheManager;

    @Transactional
    @RabbitListener(queues = RabbitMQConfig.QUEUE_CONTRATO_STATUS)
    public void processarStatusContrato(String payload) {
        try {
            log.info("Atualização de contrato recebida: {}", payload);

            JsonNode envelope = objectMapper.readTree(payload);
            if (!"CONTRATO_STATUS_ALTERADO".equals(envelope.path("eventType").asText()) || envelope.path("version").asInt() != 1) {
                throw new IllegalArgumentException("Envelope de evento não suportado");
            }
            JsonNode jsonNode = envelope.path("data");
            UUID eventId = UUID.fromString(envelope.path("eventId").asText());
            if (inboxEventRepository.existsById(eventId)) { return; }
            LocalDateTime occurredAt = Instant.parse(envelope.path("occurredAt").asText()).atZone(ZoneOffset.UTC).toLocalDateTime();
            String aggregateId = envelope.path("aggregateId").asText();
            if (inboxEventRepository.findTopByAggregateIdOrderByOccurredAtDesc(aggregateId)
                    .map(previous -> previous.getOccurredAt().isAfter(occurredAt)).orElse(false)) {
                log.warn("Evento fora de ordem ignorado: {}", eventId);
                inboxEventRepository.save(InboxEvent.builder().eventId(eventId).eventType(envelope.path("eventType").asText())
                        .aggregateId(aggregateId).occurredAt(occurredAt).processedAt(LocalDateTime.now(ZoneOffset.UTC)).build());
                return;
            }

            // Blindagem contra JSONs mal formados
            if (!jsonNode.has("propostaId") || !jsonNode.has("status")) {
                log.error("Mensagem inválida recebida: campos obrigatórios ausentes. Payload: {}", payload);
                throw new IllegalArgumentException("Campos obrigatórios ausentes");
            }

            String propostaIdStr = jsonNode.get("propostaId").asText();
            String statusContrato = jsonNode.get("status").asText();

            Proposta proposta = propostaRepository.findById(UUID.fromString(propostaIdStr))
                    .orElseThrow(() -> new RuntimeException("Proposta não encontrada para o ID: " + propostaIdStr));

            // Mapeia o status do Contrato (SIGN) para o status da Proposta (CRM)[cite: 1]
            if ("CONCLUIDO".equals(statusContrato)) {
                proposta.setStatus(PropostaStatus.ASSINATURA_CONCLUIDA);
            } else if ("CANCELADO".equals(statusContrato)) {
                proposta.setStatus(PropostaStatus.ASSINATURA_RECUSADA);
            } else if ("AGUARDANDO_ASSINATURA".equals(statusContrato)) {
                proposta.setStatus(PropostaStatus.ENVIADA_PARA_ASSINATURA);
            }

            propostaRepository.save(proposta);
            inboxEventRepository.save(InboxEvent.builder().eventId(eventId).eventType(envelope.path("eventType").asText())
                    .aggregateId(aggregateId).occurredAt(occurredAt)
                    .processedAt(LocalDateTime.now(ZoneOffset.UTC)).build());
            var cache = cacheManager.getCache("propostas");
            if (cache != null) cache.evict(UUID.fromString(propostaIdStr));
            var queryCache = cacheManager.getCache("propostas-por-cliente");
            if (queryCache != null) queryCache.clear();
            log.info("Proposta {} atualizada para o status: {}", propostaIdStr, proposta.getStatus());

        } catch (Exception e) {
            log.error("Erro ao processar atualização de status do contrato", e);
            throw new RuntimeException("Falha no processamento", e);
        }
    }
}
