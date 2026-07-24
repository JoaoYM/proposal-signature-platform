package com.powermobile.crm.api.dto;

import com.powermobile.crm.domain.model.Proposta;
import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
public class PropostaResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private UUID id;
    private String clienteNome;
    private String clienteEmail;
    private String status;
    private List<ItemResponseDTO> itens;

    // Método utilitário para converter a Proposta e seus Itens -> DTO
    public static PropostaResponseDTO fromEntity(Proposta proposta) {
        PropostaResponseDTO dto = new PropostaResponseDTO();
        dto.setId(proposta.getId());
        dto.setClienteNome(proposta.getClienteNome());
        dto.setClienteEmail(proposta.getClienteEmail());
        
        if (proposta.getStatus() != null) {
            dto.setStatus(proposta.getStatus().name()); 
        }

        if (proposta.getItens() != null) {
            dto.setItens(proposta.getItens().stream()
                    .map(ItemResponseDTO::fromEntity)
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}