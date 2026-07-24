package com.powermobile.sign.service.impl;

import com.powermobile.sign.api.dto.ContratoConcluidoEventDTO;
import com.powermobile.sign.domain.enums.AcaoAuditoria;
import com.powermobile.sign.domain.enums.ContratoStatus;
import com.powermobile.sign.domain.exception.AssinaturaDomainException;
import com.powermobile.sign.domain.exception.ContratoNotFoundException;
import com.powermobile.sign.domain.model.Assinatura;
import com.powermobile.sign.domain.model.Contrato;
import com.powermobile.sign.domain.model.ContratoEvent;
import com.powermobile.sign.domain.model.Participante;
import com.powermobile.sign.domain.port.in.ContratoUseCase;
import com.powermobile.sign.domain.port.out.EventPublisher;
import com.powermobile.sign.domain.repository.ContratoEventRepository;
import com.powermobile.sign.domain.repository.ContratoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContratoServiceImpl implements ContratoUseCase {

    private final ContratoRepository contratoRepository;
    private final ContratoEventRepository auditoriaRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public void gerarContratoDaProposta(String propostaId, String clienteNome, String clienteEmail) {
        log.info("Gerando contrato para a proposta: {}", propostaId);

        Contrato contrato = Contrato.builder()
                .propostaId(propostaId)
                .conteudo("{ \"cliente\": \"" + clienteNome + "\", \"termos\": \"Termos padrão de serviço\" }")
                .status(ContratoStatus.AGUARDANDO_ASSINATURA)
                .build();

        Participante cliente = Participante.builder()
                .nome(clienteNome)
                .email(clienteEmail)
                .ordem(1)
                // A assinatura inicializa como null
                .build();

        Participante empresa = Participante.builder()
                .nome("Diretor Power Mobile")
                .email("diretor@powermobile.com")
                .ordem(2)
                .build();

        contrato.adicionarParticipante(cliente);
        contrato.adicionarParticipante(empresa);

        Contrato contratoSalvo = contratoRepository.save(contrato);

        try {
            registrarAuditoria(contratoSalvo.getId().toString(), AcaoAuditoria.CONTRATO_GERADO, "Sistema",
            "Contrato criado a partir da proposta");
            log.info("🎯 CONTRATO GERADO! Copie este ID para a assinatura: {}", contratoSalvo.getId());
        } catch (Exception e) {
            log.error("Erro ao registrar auditoria para o contrato: {}", contratoSalvo.getId(), e);
            throw new RuntimeException("Falha ao registrar auditoria", e);
        }
    }

    @Override
    public Contrato buscarPorId(UUID id) {
        return contratoRepository.findById(id)
                .orElseThrow(() -> new ContratoNotFoundException(id));
    }

    @Override
    @Transactional
    @CacheEvict(value = "contratos", key = "#contratoId")
    public void processarAssinatura(UUID contratoId, String emailParticipante, boolean aceitou) {
        Contrato contrato = contratoRepository.findById(contratoId)
                .orElseThrow(() -> new ContratoNotFoundException(contratoId));

        if (contrato.getStatus() != ContratoStatus.AGUARDANDO_ASSINATURA) {
            throw new AssinaturaDomainException("Contrato não está mais em fase de assinatura.");
        }

        Participante atual = contrato.getParticipantes().stream()
                .filter(p -> p.getEmail().equals(emailParticipante))
                .findFirst()
                .orElseThrow(() -> new AssinaturaDomainException("Participante não faz parte deste contrato"));

        if (atual.jaAssinou()) {
            throw new AssinaturaDomainException("Participante já assinou este contrato.");
        }

        boolean turnoCorreto = contrato.getParticipantes().stream()
                .filter(p -> p.getOrdem() < atual.getOrdem())
                .allMatch(Participante::jaAssinou); 

        if (!turnoCorreto) {
            throw new AssinaturaDomainException("Não é o turno deste participante assinar. Aguarde o anterior.");
        }

        if (aceitou) {
            Assinatura assinaturaFisica = Assinatura.builder()
                    .dataHora(LocalDateTime.now())
                    .ipOrigem("127.0.0.1") 
                    .hashValidacao(UUID.randomUUID().toString())
                    .build();

            atual.registrarAssinatura(assinaturaFisica);

            registrarAuditoria(contratoId.toString(), AcaoAuditoria.ASSINATURA_REGISTRADA, atual.getNome(),
                    "Assinatura realizada com sucesso");

            boolean todosAssinaram = contrato.getParticipantes().stream().allMatch(Participante::jaAssinou);
            if (todosAssinaram) {
                contrato.setStatus(ContratoStatus.CONCLUIDO);
                registrarAuditoria(contratoId.toString(), AcaoAuditoria.CONTRATO_FINALIZADO, "Sistema",
                        "Todas as assinaturas concluídas");
                notificarCrmAlteracaoStatus(contrato);
            }
        } else {
            contrato.setStatus(ContratoStatus.CANCELADO);
            registrarAuditoria(contratoId.toString(), AcaoAuditoria.ASSINATURA_RECUSADA, atual.getNome(),
                    "Participante recusou o contrato");
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
        ContratoConcluidoEventDTO evento = new ContratoConcluidoEventDTO(
                contrato.getPropostaId(), 
                contrato.getStatus().name()
        );
        eventPublisher.publish("CONTRATO_STATUS", contrato.getId().toString(), evento);
    }
}