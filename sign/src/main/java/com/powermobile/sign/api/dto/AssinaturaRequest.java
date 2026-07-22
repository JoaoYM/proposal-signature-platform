package com.powermobile.sign.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssinaturaRequest(
    @NotBlank(message = "O email do participante é obrigatório")
    @Email(message = "Formato de email inválido")
    String email,

    @NotNull(message = "A decisão da assinatura é obrigatória")
    Boolean aceitou
) {}