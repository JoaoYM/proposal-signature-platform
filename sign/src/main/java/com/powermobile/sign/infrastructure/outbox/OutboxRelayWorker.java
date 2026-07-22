package com.powermobile.sign.infrastructure.outbox;

import com.powermobile.sign.domain.model.OutboxEvent;
import com.powermobile.sign.domain.repository.OutboxEventRepository;
import com.powermobile.sign.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class OutboxRelayWorker {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processarEventosPendentes() {
        List<OutboxEvent> eventos = outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING");

        for (OutboxEvent evento : eventos) {
            try {
                // Envia o status do contrato de volta para o CRM
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_NAME, 
                        RabbitMQConfig.ROUTING_KEY_CONTRATO_STATUS, 
                        evento.getPayload()
                );

                evento.setStatus("PROCESSED");
                outboxEventRepository.save(evento);
                log.info("Status do contrato atualizado no RabbitMQ (Evento ID: {})", evento.getId());
                
            } catch (Exception e) {
                log.error("Falha ao publicar status do contrato: {}", e.getMessage());
            }
        }
    }
}