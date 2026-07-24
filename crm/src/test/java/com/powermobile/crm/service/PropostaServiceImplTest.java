package com.powermobile.crm.service;

import com.powermobile.crm.api.dto.PropostaResponseDTO;
import com.powermobile.crm.domain.enums.PropostaStatus;
import com.powermobile.crm.domain.model.Proposta;
import com.powermobile.crm.domain.port.out.EventPublisher;
import com.powermobile.crm.domain.repository.PropostaRepository;
import com.powermobile.crm.service.impl.PropostaServiceImpl;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class PropostaServiceImplTest {

    @Mock
    private PropostaRepository propostaRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private PropostaServiceImpl propostaService;

    @Test
    @DisplayName("Deve criar uma proposta e gerar um evento no Outbox com status PENDING")
    void deveCriarPropostaEGerarOutbox() {
        // Arrange
        Proposta novaProposta = new Proposta();
        novaProposta.setClienteNome("João");
        novaProposta.setClienteEmail("joao@teste.com");
        
        // Simulando o comportamento do repositório ao salvar
        when(propostaRepository.save(any(Proposta.class))).thenAnswer(invocation -> {
            Proposta p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setStatus(PropostaStatus.CRIADA);
            return p;
        });

        // Act
        Proposta propostaSalva = propostaService.criarProposta(novaProposta);

        // Assert
        assertNotNull(propostaSalva.getId(), "A proposta deveria ter um ID gerado");
        assertEquals(PropostaStatus.CRIADA, propostaSalva.getStatus());

        // Verifica se o repositório da proposta foi chamado 1 vez
        verify(propostaRepository, times(1)).save(any(Proposta.class));

        // Captura o DTO publicado no EventPublisher para inspecionar seus valores
        ArgumentCaptor<PropostaResponseDTO> dtoCaptor = ArgumentCaptor.forClass(PropostaResponseDTO.class);
        verify(eventPublisher, times(1)).publish(eq("PROPOSTA"), anyString(), dtoCaptor.capture());

        PropostaResponseDTO eventoPublicado = dtoCaptor.getValue();
        assertEquals("João", eventoPublicado.getClienteNome(), "O DTO do evento deve conter os dados do cliente");
    }
}