package com.powermobile.sign.api.dto;

import com.powermobile.sign.domain.model.Participante;
import lombok.Builder;
import lombok.Data;
import java.io.Serializable;

@Data
@Builder
public class ParticipanteResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nome;
    private String email;
    private Integer ordem;
    private Boolean assinou;
    
    private AssinaturaResponseDTO assinatura;

    public static ParticipanteResponseDTO fromEntity(Participante participante) {
        return ParticipanteResponseDTO.builder()
                .nome(participante.getNome())
                .email(participante.getEmail())
                .ordem(participante.getOrdem())
                .assinou(participante.jaAssinou())
                // Delega a conversão para o DTO específico, passando o Value Object
                .assinatura(AssinaturaResponseDTO.fromEntity(participante.getAssinatura()))
                .build();
    }
}