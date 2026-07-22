package com.powermobile.sign.domain.repository;

import com.powermobile.sign.domain.model.ContratoEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContratoEventRepository extends JpaRepository<ContratoEvent, UUID> {
}