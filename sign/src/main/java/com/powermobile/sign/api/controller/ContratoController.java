package com.powermobile.sign.api.controller;

import com.powermobile.sign.api.dto.AssinaturaRequest;
import com.powermobile.sign.api.dto.ContratoResponseDTO;
import com.powermobile.sign.domain.model.Contrato;
import com.powermobile.sign.domain.port.in.ContratoUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contratos")
@RequiredArgsConstructor
public class ContratoController {

    private final ContratoUseCase contratoUseCase;

    @PostMapping("/{id}/assinaturas")
    public ResponseEntity<String> processarAssinatura(
            @PathVariable UUID id,
            @RequestBody @Valid AssinaturaRequest request) {

        // A regra de negócio, ordem sequencial e auditoria estão todas encapsuladas no serviço
        contratoUseCase.processarAssinatura(id, request.email(), request.aceitou());

        String mensagem = request.aceitou() 
                ? "Assinatura registrada com sucesso." 
                : "Assinatura recusada. Fluxo do contrato cancelado.";

        return ResponseEntity.ok(mensagem);
    }

    @GetMapping("/{id}")
    @Cacheable(value = "contratos", key = "#id")
    @ResponseStatus(HttpStatus.OK)
    public ContratoResponseDTO buscarContratoPorId(@PathVariable UUID id) {
        Contrato contrato = contratoUseCase.buscarPorId(id);
        return ContratoResponseDTO.fromEntity(contrato);
    }
}