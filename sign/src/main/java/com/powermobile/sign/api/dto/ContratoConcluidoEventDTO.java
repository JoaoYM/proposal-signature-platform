package com.powermobile.sign.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratoConcluidoEventDTO {
    private String propostaId;
    private String status;
}