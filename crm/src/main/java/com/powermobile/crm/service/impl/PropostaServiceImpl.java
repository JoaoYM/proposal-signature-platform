package com.powermobile.crm.service.impl;

import com.powermobile.crm.api.dto.PropostaResponseDTO;
import com.powermobile.crm.domain.enums.PropostaStatus;
import com.powermobile.crm.domain.exception.PropostaNotFoundException;
import com.powermobile.crm.domain.model.Proposta;
import com.powermobile.crm.domain.port.out.EventPublisher;
import com.powermobile.crm.domain.repository.PropostaRepository;
import com.powermobile.crm.service.PropostaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PropostaServiceImpl implements PropostaService {

    private final PropostaRepository propostaRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public Proposta criarProposta(Proposta proposta) {
        log.info("Iniciando criacao de proposta para o cliente: {}", proposta.getClienteNome());

        // 1. Define o estado inicial exigido
        proposta.setStatus(PropostaStatus.CRIADA);

        // Garante a bidirecionalidade dos itens
        if (proposta.getItens() != null) {
            proposta.getItens().forEach(item -> item.setProposta(proposta));
        }

        Proposta propostaSalva = propostaRepository.save(proposta);

        // 2. Registra o evento no Outbox para integração futura com o SIGN
        registrarEventoOutbox(propostaSalva);

        return propostaSalva;
    }

    @Override
    public Proposta buscarPorId(UUID id) {
        return propostaRepository.findById(id)
                .orElseThrow(() -> new PropostaNotFoundException(id));
    }

    @Override
    public Page<Proposta> buscarPorCliente(String clienteNome, Pageable pageable) {
        return propostaRepository.findByClienteNomeContainingIgnoreCase(clienteNome, pageable);
    }

    private void registrarEventoOutbox(Proposta proposta) {
        PropostaResponseDTO eventoDto = PropostaResponseDTO.fromEntity(proposta);
        eventPublisher.publish("PROPOSTA", proposta.getId().toString(), eventoDto);
    }
}