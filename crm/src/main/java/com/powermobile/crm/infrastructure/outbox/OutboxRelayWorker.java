package com.powermobile.crm.infrastructure.outbox;

import com.powermobile.crm.domain.model.OutboxEvent;
import com.powermobile.crm.domain.repository.OutboxEventRepository;
import com.powermobile.crm.infrastructure.config.RabbitMQConfig;
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

    // Executa a cada 5 segundos
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processarEventosPendentes() {
        List<OutboxEvent> eventosPendentes = outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING");

        if (eventosPendentes.isEmpty()) {
            return;
        }

        log.info("Encontrados {} eventos pendentes no Outbox. Iniciando processamento...", eventosPendentes.size());

        for (OutboxEvent evento : eventosPendentes) {
            try {
                // Envia a proposta para o sistema de assinatura (SIGN)
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_NAME, 
                        RabbitMQConfig.ROUTING_KEY_PROPOSTA_CRIADA, 
                        evento.getPayload()
                );

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