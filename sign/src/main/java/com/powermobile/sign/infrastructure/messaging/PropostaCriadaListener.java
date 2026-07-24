package com.powermobile.sign.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powermobile.sign.domain.port.in.ContratoUseCase;
import com.powermobile.sign.domain.repository.ContratoRepository;
import com.powermobile.sign.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PropostaCriadaListener {

    private final ContratoUseCase contratoUseCase;
    private final ContratoRepository contratoRepository;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_PROPOSTA_CRIADA)
    public void processarPropostaCriada(String payload) {
        try {
            log.info("Evento recebido do CRM: {}", payload);
            
            // Leitura do JSON que veio do Outbox do CRM
            JsonNode jsonNode = objectMapper.readTree(payload);
            String propostaId = jsonNode.get("id").asText();
            String clienteNome = jsonNode.get("clienteNome").asText();
            String clienteEmail = jsonNode.get("clienteEmail").asText();

            // IDEMPOTÊNCIA: Verifica se já existe um contrato para esta proposta no banco de dados.
            if (contratoRepository.findByPropostaId(propostaId).isPresent()) {
                log.warn("Contrato já existe para a proposta {}. Evento ignorado.", propostaId);
                return;
            }

            contratoUseCase.gerarContratoDaProposta(propostaId, clienteNome, clienteEmail);
            log.info("Contrato gerado com sucesso para a proposta {}", propostaId);

        } catch (Exception e) {
            log.error("Erro ao processar evento de proposta criada", e);
            // Ao lançar a exceção, o Spring AMQP sinaliza NACK para o RabbitMQ,
            // que pode recolocar a mensagem na fila ou enviá-la para uma Dead Letter Queue (DLQ).
            throw new RuntimeException("Falha no processamento da mensagem", e);
        }
    }
}