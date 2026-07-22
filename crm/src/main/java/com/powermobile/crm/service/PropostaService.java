package com.powermobile.crm.service;

import com.powermobile.crm.domain.model.Proposta;
import java.util.List;
import java.util.UUID;

public interface PropostaService {
    Proposta criarProposta(Proposta proposta);
    Proposta buscarPorId(UUID id);
    List<Proposta> buscarPorCliente(String clienteNome);
}