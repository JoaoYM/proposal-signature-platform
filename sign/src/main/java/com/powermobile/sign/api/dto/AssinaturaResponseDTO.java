package com.powermobile.sign.api.dto;

import com.powermobile.sign.domain.model.Assinatura;
import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class AssinaturaResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private LocalDateTime dataHora;
    private String ipOrigem;
    private String hashValidacao;

    public static AssinaturaResponseDTO fromEntity(Assinatura assinatura) {
        if (assinatura == null) {
            return null;
        }
        
        return AssinaturaResponseDTO.builder()
                .dataHora(assinatura.getDataHora())
                .ipOrigem(assinatura.getIpOrigem())
                .hashValidacao(assinatura.getHashValidacao())
                .build();
    }
}