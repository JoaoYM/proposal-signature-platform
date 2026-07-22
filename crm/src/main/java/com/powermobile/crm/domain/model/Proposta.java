package com.powermobile.crm.domain.model;

import com.powermobile.crm.domain.enums.PropostaStatus;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "propostas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Proposta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "cliente_nome", nullable = false)
    private String clienteNome;

    @Column(name = "cliente_email", nullable = false)
    private String clienteEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropostaStatus status;

    // Controle de Concorrência (Lock Otimista)
    @Version
    private Long version;

    // Relacionamento com os itens da proposta
    @OneToMany(mappedBy = "proposta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemProposta> itens = new ArrayList<>();

    // Método utilitário para manter a consistência bidirecional
    public void adicionarItem(ItemProposta item) {
        itens.add(item);
        item.setProposta(this);
    }
}