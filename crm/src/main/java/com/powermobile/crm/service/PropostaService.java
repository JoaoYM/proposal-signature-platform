package com.powermobile.crm.service;

import com.powermobile.crm.domain.model.Proposta;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PropostaService {
    Proposta criarProposta(Proposta proposta);
    Proposta buscarPorId(UUID id);
    Page<Proposta> buscarPorCliente(String clienteNome, Pageable pageable);
}