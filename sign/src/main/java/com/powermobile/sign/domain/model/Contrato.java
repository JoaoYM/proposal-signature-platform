package com.powermobile.sign.domain.model;

import com.powermobile.sign.domain.enums.ContratoStatus;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "contratos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "proposta_id", nullable = false, unique = true)
    private String propostaId;

    @Column(columnDefinition = "TEXT", nullable = false, updatable = false)
    private String conteudo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContratoStatus status;

    @Version
    private Long version;

    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("ordem ASC")
    private List<Participante> participantes = new ArrayList<>();

    public void adicionarParticipante(Participante participante) {
        participantes.add(participante);
        participante.setContrato(this);
    }
}
