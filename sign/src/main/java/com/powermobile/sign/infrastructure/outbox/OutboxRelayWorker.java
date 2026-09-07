package com.powermobile.sign.infrastructure.outbox;

import com.powermobile.sign.domain.model.OutboxEvent;
import com.powermobile.sign.domain.repository.OutboxEventRepository;
import com.powermobile.sign.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class OutboxRelayWorker {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 5000)
    public void processarEventosPendentes() {
        List<OutboxEvent> eventos = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");

        for (OutboxEvent evento : eventos) {
            try {
                // Envia o status do contrato de volta para o CRM
                CorrelationData correlation = new CorrelationData(evento.getId().toString());
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME,
                        RabbitMQConfig.ROUTING_KEY_CONTRATO_STATUS, evento.getPayload(), correlation);
                CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirm.ack() || correlation.getReturned() != null) {
                    throw new IllegalStateException("Broker não confirmou o roteamento: " + confirm.reason());
                }

                evento.setStatus("PROCESSED");
                outboxEventRepository.save(evento);
                log.info("Status do contrato atualizado no RabbitMQ (Evento ID: {})", evento.getId());
                
            } catch (Exception e) {
                log.error("Falha ao publicar status do contrato: {}", e.getMessage());
            }
        }
    }
}
