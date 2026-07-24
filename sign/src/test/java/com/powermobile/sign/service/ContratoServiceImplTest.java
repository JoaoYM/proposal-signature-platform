package com.powermobile.sign.service;

import com.powermobile.sign.domain.enums.ContratoStatus;
import com.powermobile.sign.domain.model.Contrato;
import com.powermobile.sign.domain.model.ContratoEvent;
import com.powermobile.sign.domain.model.Participante;
import com.powermobile.sign.domain.port.out.EventPublisher;
import com.powermobile.sign.domain.repository.ContratoEventRepository;
import com.powermobile.sign.domain.repository.ContratoRepository;
import com.powermobile.sign.domain.repository.OutboxEventRepository;
import com.powermobile.sign.domain.exception.AssinaturaDomainException;
import com.powermobile.sign.service.impl.ContratoServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.mockito.Spy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContratoServiceImplTest {

    @Mock
    private ContratoRepository contratoRepository;

    @Mock
    private ContratoEventRepository auditoriaRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private ContratoServiceImpl contratoService;

    @Test
    @DisplayName("Deve barrar a assinatura se o participante tentar assinar fora do seu turno")
    void deveBarrarAssinaturaForaDeOrdem() {
        // Arrange
        UUID contratoId = UUID.randomUUID();

        Participante p1 = new Participante();
        p1.setEmail("cliente@teste.com");
        p1.setOrdem(1);

        Participante p2 = new Participante();
        p2.setEmail("diretor@powermobile.com");
        p2.setOrdem(2);

        Contrato contrato = new Contrato();
        contrato.setId(contratoId);
        contrato.setStatus(ContratoStatus.AGUARDANDO_ASSINATURA);
        contrato.setParticipantes(List.of(p1, p2));

        when(contratoRepository.findById(contratoId)).thenReturn(Optional.of(contrato));

        // Act & Assert
        // O Diretor (ordem 2) tenta assinar, mas o Cliente (ordem 1) ainda não assinou
        AssinaturaDomainException exception = assertThrows(AssinaturaDomainException.class, () -> {
            contratoService.processarAssinatura(contratoId, "diretor@powermobile.com", true);
        });

        assertEquals("Não é o turno deste participante assinar. Aguarde o anterior.", exception.getMessage());

        // Verifica que NUNCA salvou o contrato (pois deu erro)
        verify(contratoRepository, never()).save(any());
        verify(auditoriaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve processar a assinatura do primeiro participante com sucesso")
    void deveProcessarAssinaturaNaOrdemCorreta() {
        // Arrange
        UUID contratoId = UUID.randomUUID();

        Participante p1 = new Participante();
        p1.setEmail("cliente@teste.com");
        p1.setOrdem(1);

        Contrato contrato = new Contrato();
        contrato.setId(contratoId);
        contrato.setStatus(ContratoStatus.AGUARDANDO_ASSINATURA);
        contrato.setParticipantes(List.of(p1));

        when(contratoRepository.findById(contratoId)).thenReturn(Optional.of(contrato));

        // Act
        contratoService.processarAssinatura(contratoId, "cliente@teste.com", true);

        // Assert
        assertTrue(p1.jaAssinou(), "O participante deve constar como tendo assinado (assinatura != null)");
        assertNotNull(p1.getAssinatura(), "O objeto rico de Assinatura deve ter sido instanciado");
        assertNotNull(p1.getAssinatura().getDataHora(), "A data da assinatura deve ter sido preenchida");
        
        verify(contratoRepository, times(1)).save(contrato);
        verify(auditoriaRepository, times(2)).save(any(ContratoEvent.class));
    }
}