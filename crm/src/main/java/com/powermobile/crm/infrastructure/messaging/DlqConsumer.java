package com.powermobile.crm.infrastructure.messaging;

import com.powermobile.crm.infrastructure.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DlqConsumer {

    @RabbitListener(queues = RabbitMQConfig.DLQ_PROPOSTA_CRIADA)
    public void processarPropostaCriadaDlq(String payload) {
        log.error("Mensagem enviada para DLQ (proposta.criada): {}", payload);
    }

    @RabbitListener(queues = RabbitMQConfig.DLQ_CONTRATO_STATUS)
    public void processarContratoStatusDlq(String payload) {
        log.error("Mensagem enviada para DLQ (contrato.status): {}", payload);
    }
}