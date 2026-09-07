package com.powermobile.sign.domain.port.in;

import com.powermobile.sign.domain.model.Contrato;
import java.util.UUID;

public interface ContratoUseCase {
    void gerarContratoDaProposta(String propostaId, String clienteNome, String clienteEmail);
    void processarAssinatura(UUID contratoId, String emailParticipante, boolean aceitou);
    void processarAssinatura(UUID contratoId, String emailParticipante, boolean aceitou, String ipOrigem);
    Contrato buscarPorId(UUID id);
    Contrato buscarPorPropostaId(String propostaId);
}
