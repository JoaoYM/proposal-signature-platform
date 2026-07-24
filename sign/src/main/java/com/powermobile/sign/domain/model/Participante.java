package com.powermobile.sign.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "participantes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private Integer ordem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id")
    private Contrato contrato;

    @Embedded
    private Assinatura assinatura;

    // MÉTODOS DE NEGÓCIO (Rich Domain Model)

    public boolean jaAssinou() {
        return this.assinatura != null;
    }

    public void registrarAssinatura(Assinatura novaAssinatura) {
        if (jaAssinou()) {
            throw new IllegalStateException("Participante já assinou este contrato.");
        }
        this.assinatura = novaAssinatura;
    }
}