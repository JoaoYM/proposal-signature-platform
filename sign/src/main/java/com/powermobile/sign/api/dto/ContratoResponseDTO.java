package com.powermobile.sign.api.dto;

import com.powermobile.sign.domain.model.Contrato;
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.stream.Collectors;
import java.io.Serializable;

@Data
@Builder
public class ContratoResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String propostaId;
    private String status;
    private List<ParticipanteResponseDTO> participantes;

    public static ContratoResponseDTO fromEntity(Contrato contrato) {
        return ContratoResponseDTO.builder()
                .id(contrato.getId().toString())
                .propostaId(contrato.getPropostaId()) 
                .status(contrato.getStatus().name())
                .participantes(contrato.getParticipantes() != null ? 
                        contrato.getParticipantes().stream()
                                .map(ParticipanteResponseDTO::fromEntity)
                                .collect(Collectors.toList()) : null)
                .build();
    }
}