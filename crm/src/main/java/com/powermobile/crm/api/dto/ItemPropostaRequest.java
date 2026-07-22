package com.powermobile.crm.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ItemPropostaRequest(
    @NotBlank(message = "O nome do item é obrigatório")
    String nome,
    
    @NotNull(message = "A quantidade é obrigatória")
    @Min(value = 1, message = "A quantidade deve ser maior que zero")
    Integer quantidade,
    
    @NotNull(message = "O preço unitário é obrigatório")
    @Min(value = 0, message = "O preço não pode ser negativo")
    BigDecimal precoUnitario
) {}