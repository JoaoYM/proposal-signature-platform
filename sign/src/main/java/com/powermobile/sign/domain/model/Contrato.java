package com.powermobile.sign.domain.model;

import com.powermobile.sign.domain.enums.ContratoStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Version;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OrderBy;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

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

    @Column(columnDefinition = "TEXT", nullable = false)
    private String conteudo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContratoStatus status;

    @Version
    private Long version;

    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("ordem ASC") // Garante que a lista venha ordenada do banco
    private List<Participante> participantes = new ArrayList<>();

    public void adicionarParticipante(Participante participante) {
        participantes.add(participante);
        participante.setContrato(this);
    }
}