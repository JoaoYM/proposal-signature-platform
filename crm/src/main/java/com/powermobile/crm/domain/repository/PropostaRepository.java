package com.powermobile.crm.domain.repository;

import com.powermobile.crm.domain.model.Proposta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

@Repository
public interface PropostaRepository extends JpaRepository<Proposta, UUID> {
    Page<Proposta> findByClienteNomeContainingIgnoreCase(String clienteNome, Pageable pageable);
}