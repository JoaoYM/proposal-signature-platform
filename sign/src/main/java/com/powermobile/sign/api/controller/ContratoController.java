package com.powermobile.sign.api.controller;

import com.powermobile.sign.api.dto.AssinaturaRequest;
import com.powermobile.sign.domain.model.Contrato;
import com.powermobile.sign.domain.repository.ContratoRepository;
import com.powermobile.sign.service.impl.ContratoServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contratos")
@RequiredArgsConstructor
public class ContratoController {

    private final ContratoServiceImpl contratoService;
    private final ContratoRepository contratoRepository;

    @PostMapping("/{id}/assinaturas")
    public ResponseEntity<String> processarAssinatura(
            @PathVariable UUID id,
            @RequestBody @Valid AssinaturaRequest request) {

        // A regra de negócio, ordem sequencial e auditoria estão todas encapsuladas no serviço
        contratoService.processarAssinatura(id, request.email(), request.aceitou());

        String mensagem = request.aceitou() 
                ? "Assinatura registrada com sucesso." 
                : "Assinatura recusada. Fluxo do contrato cancelado.";

        return ResponseEntity.ok(mensagem);
    }

    // Endpoint auxiliar útil para consultas e testes da evolução dos status
    @GetMapping("/{id}")
    public ResponseEntity<Contrato> buscarContratoPorId(@PathVariable UUID id) {
        return contratoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}