package com.powermobile.crm.api.controller;

import com.powermobile.crm.api.dto.PropostaRequest;
import com.powermobile.crm.domain.model.ItemProposta;
import com.powermobile.crm.domain.model.Proposta;
import com.powermobile.crm.service.PropostaService;
import com.powermobile.crm.api.dto.PropostaResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/propostas")
@RequiredArgsConstructor
public class PropostaController {

    private final PropostaService propostaService;

    @PostMapping
    public ResponseEntity criarProposta(@Valid @RequestBody PropostaRequest request) {

        // 1. Converter o request em entidade antes de chamar o service
        Proposta proposta = Proposta.builder()
                .clienteNome(request.clienteNome())
                .clienteEmail(request.clienteEmail())
                .itens(request.itens().stream().map(itemDto -> ItemProposta.builder()
                        .nome(itemDto.nome())
                        .quantidade(itemDto.quantidade())
                        .precoUnitario(itemDto.precoUnitario())
                        .build()).collect(Collectors.toList()))
                .build();

        Proposta propostaSalva = propostaService.criarProposta(proposta);

        // 2. Convertemos a Entidade em DTO antes de devolver na resposta
        PropostaResponseDTO responseDTO = PropostaResponseDTO.fromEntity(propostaSalva);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    @GetMapping("/{id}")
    @Cacheable(value = "propostas", key = "#id")
    @ResponseStatus(HttpStatus.OK) // Garante o retorno 200 OK
    public PropostaResponseDTO buscarPorId(@PathVariable UUID id) {
        // Busca a entidade do banco e converte para DTO
        Proposta proposta = propostaService.buscarPorId(id);
        return PropostaResponseDTO.fromEntity(proposta);
    }

    @GetMapping
    public ResponseEntity<List<PropostaResponseDTO>> buscarPorCliente(@RequestParam String clienteNome) {
        List<Proposta> propostas = propostaService.buscarPorCliente(clienteNome);
        List<PropostaResponseDTO> response = propostas.stream()
                .map(PropostaResponseDTO::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }
}