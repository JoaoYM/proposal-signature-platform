package com.powermobile.sign.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "proposta.exchange";
    public static final String QUEUE_PROPOSTA_CRIADA = "proposta.criada.queue";
    public static final String ROUTING_KEY_PROPOSTA_CRIADA = "proposta.criada.routingKey";
    public static final String QUEUE_CONTRATO_STATUS = "contrato.status.queue";
    public static final String ROUTING_KEY_CONTRATO_STATUS = "contrato.status.routingKey";
    public static final String DLQ_PROPOSTA_CRIADA = "proposta.criada.dlq";
    public static final String DLQ_CONTRATO_STATUS = "contrato.status.dlq";

    @Bean
    public DirectExchange propostaExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue propostaCriadaQueue() {
        return new Queue(QUEUE_PROPOSTA_CRIADA, true);
    }

    @Bean
    public Binding bindingPropostaCriada(Queue propostaCriadaQueue, DirectExchange propostaExchange) {
        return BindingBuilder.bind(propostaCriadaQueue).to(propostaExchange).with(ROUTING_KEY_PROPOSTA_CRIADA);
    }

    @Bean
    public Queue contratoStatusQueue() {
        return new Queue(QUEUE_CONTRATO_STATUS, true);
    }

    @Bean
    public Binding bindingContratoStatus(Queue contratoStatusQueue, DirectExchange propostaExchange) {
        return BindingBuilder.bind(contratoStatusQueue).to(propostaExchange).with(ROUTING_KEY_CONTRATO_STATUS);
    }

    @Bean
    public Queue propostaCriadaDlq() {
        return new Queue(DLQ_PROPOSTA_CRIADA, true);
    }

    @Bean
    public Queue contratoStatusDlq() {
        return new Queue(DLQ_CONTRATO_STATUS, true);
    }
}
