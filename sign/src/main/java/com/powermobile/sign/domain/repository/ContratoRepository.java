package com.powermobile.sign.domain.repository;

import com.powermobile.sign.domain.model.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, UUID> {
    Optional<Contrato> findByPropostaId(String propostaId);
}