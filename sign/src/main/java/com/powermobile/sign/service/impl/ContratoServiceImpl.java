package com.powermobile.sign.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powermobile.sign.domain.enums.AcaoAuditoria;
import com.powermobile.sign.domain.enums.ContratoStatus;
import com.powermobile.sign.domain.model.Contrato;
import com.powermobile.sign.domain.model.ContratoEvent;
import com.powermobile.sign.domain.model.OutboxEvent;
import com.powermobile.sign.domain.model.Participante;
import com.powermobile.sign.domain.repository.ContratoEventRepository;
import com.powermobile.sign.domain.repository.ContratoRepository;
import com.powermobile.sign.domain.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContratoServiceImpl {

    private final ContratoRepository contratoRepository;
    private final ContratoEventRepository auditoriaRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void gerarContratoDaProposta(String propostaId, String clienteNome, String clienteEmail) {
        log.info("Gerando contrato para a proposta: {}", propostaId);

        // 1. Cria o contrato (Simulando o JSON de conteúdo exigido)[cite: 1]
        Contrato contrato = Contrato.builder()
                .propostaId(propostaId)
                .conteudo("{ \"cliente\": \"" + clienteNome + "\", \"termos\": \"Termos padrão de serviço\" }")
                .status(ContratoStatus.AGUARDANDO_ASSINATURA)
                .build();

        // 2. Define a ordem dos participantes (Ex: Cliente assina primeiro, Empresa depois)[cite: 1]
        Participante cliente = Participante.builder()
                .nome(clienteNome)
                .email(clienteEmail)
                .ordem(1)
                .assinou(false)
                .build();

        Participante empresa = Participante.builder()
                .nome("Diretor Power Mobile")
                .email("diretor@powermobile.com")
                .ordem(2)
                .assinou(false)
                .build();

        contrato.adicionarParticipante(cliente);
        contrato.adicionarParticipante(empresa);

        Contrato salvo = contratoRepository.save(contrato);

        // 3. Registra na Auditoria[cite: 1]
        registrarAuditoria(salvo.getId().toString(), AcaoAuditoria.CONTRATO_GERADO, "Sistema", "Contrato criado a partir da proposta");
    }

    @Transactional
    public void processarAssinatura(UUID contratoId, String emailParticipante, boolean aceitou) {
        Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new RuntimeException("Contrato não encontrado"));

        if (contrato.getStatus() != ContratoStatus.AGUARDANDO_ASSINATURA) {
            throw new IllegalStateException("Contrato não está mais em fase de assinatura.");
        }

        Participante atual = contrato.getParticipantes().stream()
                .filter(p -> p.getEmail().equals(emailParticipante))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Participante não faz parte deste contrato"));

        if (atual.getAssinou()) {
            throw new IllegalStateException("Participante já assinou este contrato.");
        }

        // Valida a regra de negócio central: Ordem sequencial[cite: 1]
        boolean turnoCorreto = contrato.getParticipantes().stream()
                .filter(p -> p.getOrdem() < atual.getOrdem())
                .allMatch(Participante::getAssinou);

        if (!turnoCorreto) {
            throw new IllegalStateException("Não é o turno deste participante assinar. Aguarde o anterior.");
        }

        if (aceitou) {
            atual.setAssinou(true);
            atual.setDataAssinatura(LocalDateTime.now());
            registrarAuditoria(contratoId.toString(), AcaoAuditoria.ASSINATURA_REGISTRADA, atual.getNome(), "Assinatura realizada com sucesso");

            // Verifica se todos assinaram[cite: 1]
            boolean todosAssinaram = contrato.getParticipantes().stream().allMatch(Participante::getAssinou);
            if (todosAssinaram) {
                contrato.setStatus(ContratoStatus.CONCLUIDO);
                registrarAuditoria(contratoId.toString(), AcaoAuditoria.CONTRATO_FINALIZADO, "Sistema", "Todas as assinaturas concluídas");
                notificarCrmAlteracaoStatus(contrato);
            }
        } else {
            // Se recusou, cancela o fluxo[cite: 1]
            contrato.setStatus(ContratoStatus.CANCELADO);
            registrarAuditoria(contratoId.toString(), AcaoAuditoria.ASSINATURA_RECUSADA, atual.getNome(), "Participante recusou o contrato");
            notificarCrmAlteracaoStatus(contrato);
        }

        contratoRepository.save(contrato);
    }

    private void registrarAuditoria(String contratoId, AcaoAuditoria acao, String ator, String detalhes) {
        ContratoEvent evento = ContratoEvent.builder()
                .contratoId(contratoId)
                .acao(acao)
                .ator(ator)
                .detalhes(detalhes)
                .build();
        auditoriaRepository.save(evento);
    }

    private void notificarCrmAlteracaoStatus(Contrato contrato) {
        try {
            // Usa o Outbox para garantir a entrega da mensagem de volta ao CRM[cite: 1]
            String payload = objectMapper.writeValueAsString(contrato);
            OutboxEvent outbox = OutboxEvent.builder()
                    .aggregateType("CONTRATO_STATUS")
                    .aggregateId(contrato.getId().toString())
                    .payload(payload)
                    .status("PENDING")
                    .build();
            outboxEventRepository.save(outbox);
        } catch (Exception e) {
            log.error("Erro ao gerar evento de notificação para o CRM", e);
            throw new RuntimeException(e);
        }
    }
}