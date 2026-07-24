package com.powermobile.sign.domain.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Assinatura {
    
    private LocalDateTime dataHora;
    private String ipOrigem;
    private String hashValidacao;
    
}