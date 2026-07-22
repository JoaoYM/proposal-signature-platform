package com.powermobile.crm.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.Valid;
import java.util.List;

public record PropostaRequest(
    @NotBlank(message = "O nome do cliente é obrigatório")
    String clienteNome,
    
    @NotBlank(message = "O email do cliente é obrigatório")
    @Email(message = "Email inválido")
    String clienteEmail,
    
    @NotEmpty(message = "A proposta deve conter pelo menos um item")
    @Valid
    List<ItemPropostaRequest> itens
) {}