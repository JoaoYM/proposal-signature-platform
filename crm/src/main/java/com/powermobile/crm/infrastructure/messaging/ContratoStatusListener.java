package com.powermobile.crm.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powermobile.crm.domain.enums.PropostaStatus;
import com.powermobile.crm.domain.model.Proposta;
import com.powermobile.crm.domain.repository.PropostaRepository;
import com.powermobile.crm.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContratoStatusListener {

    private final PropostaRepository propostaRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    @RabbitListener(queues = RabbitMQConfig.QUEUE_CONTRATO_STATUS)
    public void processarStatusContrato(String payload) {
        try {
            log.info("Atualização de contrato recebida: {}", payload);
            
            JsonNode jsonNode = objectMapper.readTree(payload);
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
            log.info("Proposta {} atualizada para o status: {}", propostaIdStr, proposta.getStatus());

        } catch (Exception e) {
            log.error("Erro ao processar atualização de status do contrato", e);
            throw new RuntimeException("Falha no processamento", e);
        }
    }
}