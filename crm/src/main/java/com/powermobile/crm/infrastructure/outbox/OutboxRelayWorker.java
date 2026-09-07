package com.powermobile.crm.infrastructure.outbox;

import com.powermobile.crm.domain.model.OutboxEvent;
import com.powermobile.crm.domain.repository.OutboxEventRepository;
import com.powermobile.crm.infrastructure.config.RabbitMQConfig;
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

    // Executa a cada 5 segundos
    @Scheduled(fixedDelay = 5000)
    public void processarEventosPendentes() {
        List<OutboxEvent> eventosPendentes = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");

        if (eventosPendentes.isEmpty()) {
            return;
        }

        log.info("Encontrados {} eventos pendentes no Outbox. Iniciando processamento...", eventosPendentes.size());

        for (OutboxEvent evento : eventosPendentes) {
            try {
                // Envia a proposta para o sistema de assinatura (SIGN)
                CorrelationData correlation = new CorrelationData(evento.getId().toString());
                rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME,
                        RabbitMQConfig.ROUTING_KEY_PROPOSTA_CRIADA, evento.getPayload(), correlation);
                CorrelationData.Confirm confirm = correlation.getFuture().get(5, TimeUnit.SECONDS);
                if (!confirm.ack() || correlation.getReturned() != null) {
                    throw new IllegalStateException("Broker não confirmou o roteamento: " + confirm.reason());
                }

                // Atualiza o status para não ser processado novamente
                evento.setStatus("PROCESSED");
                outboxEventRepository.save(evento);
                
                log.info("Evento ID {} publicado com sucesso e marcado como PROCESSED.", evento.getId());
                
            } catch (Exception e) {
                // Se o RabbitMQ estiver fora do ar, o log registra, a transação faz rollback desse evento específico,
                // e ele tentará novamente na próxima execução do @Scheduled.
                log.error("Falha ao publicar evento ID {} no RabbitMQ: {}", evento.getId(), e.getMessage());
            }
        }
    }
}
