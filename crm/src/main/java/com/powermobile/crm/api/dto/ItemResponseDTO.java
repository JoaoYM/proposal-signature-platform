package com.powermobile.crm.api.dto;

import com.powermobile.crm.domain.model.ItemProposta;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;
import java.io.Serializable;

@Data
public class ItemResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String nome;
    private Integer quantidade;
    private BigDecimal precoUnitario;

    // Método utilitário para converter Entidade -> DTO
    public static ItemResponseDTO fromEntity(ItemProposta item) {
        ItemResponseDTO dto = new ItemResponseDTO();
        dto.setId(item.getId());
        dto.setNome(item.getNome());
        dto.setQuantidade(item.getQuantidade());
        dto.setPrecoUnitario(item.getPrecoUnitario());
        return dto;
    }
}