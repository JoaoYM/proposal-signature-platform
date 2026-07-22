package com.powermobile.crm.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powermobile.crm.domain.enums.PropostaStatus;
import com.powermobile.crm.domain.model.OutboxEvent;
import com.powermobile.crm.domain.model.Proposta;
import com.powermobile.crm.domain.repository.OutboxEventRepository;
import com.powermobile.crm.domain.repository.PropostaRepository;
import com.powermobile.crm.service.PropostaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropostaServiceImpl implements PropostaService {

    private final PropostaRepository propostaRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional // Garante o ACID: Salva a proposta E o evento, ou faz rollback de tudo
    public Proposta criarProposta(Proposta proposta) {
        log.info("Iniciando criacao de proposta para o cliente: {}", proposta.getClienteNome());

        // 1. Define o estado inicial exigido
        proposta.setStatus(PropostaStatus.CRIADA);

        // Garante a bidirecionalidade dos itens (boa prática do JPA)
        if (proposta.getItens() != null) {
            proposta.getItens().forEach(item -> item.setProposta(proposta));
        }

        // 2. Salva a proposta no banco de dados
        Proposta propostaSalva = propostaRepository.save(proposta);

        // 3. Registra o evento no Outbox para integração futura com o SIGN
        registrarEventoOutbox(propostaSalva);

        return propostaSalva;
    }

    @Override
    public Proposta buscarPorId(UUID id) {
        return propostaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proposta não encontrada: " + id));
    }

    @Override
    public List<Proposta> buscarPorCliente(String clienteNome) {
        return propostaRepository.findByClienteNomeContainingIgnoreCase(clienteNome);
    }

    private void registrarEventoOutbox(Proposta proposta) {
        try {
            String payload = objectMapper.writeValueAsString(proposta);

            OutboxEvent evento = OutboxEvent.builder()
                    .aggregateType("PROPOSTA")
                    .aggregateId(proposta.getId().toString())
                    .payload(payload)
                    .status("PENDING")
                    .build();

            outboxEventRepository.save(evento);
            log.info("Evento PENDING registrado no Outbox para a proposta: {}", proposta.getId());
            
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar payload da proposta para o Outbox", e);
            throw new RuntimeException("Falha na integridade do evento", e);
        }
    }
}