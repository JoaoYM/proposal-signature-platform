package com.powermobile.crm.domain.repository;

import com.powermobile.crm.domain.model.Proposta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PropostaRepository extends JpaRepository<Proposta, UUID> {
    
    // Atende ao requisito: "Consultar propostas por [...] cliente"
    List<Proposta> findByClienteNomeContainingIgnoreCase(String clienteNome);
}