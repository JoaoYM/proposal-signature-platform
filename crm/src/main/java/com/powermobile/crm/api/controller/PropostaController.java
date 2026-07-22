package com.powermobile.crm.api.controller;

import com.powermobile.crm.api.dto.PropostaRequest;
import com.powermobile.crm.domain.model.ItemProposta;
import com.powermobile.crm.domain.model.Proposta;
import com.powermobile.crm.service.PropostaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    public ResponseEntity<Proposta> criarProposta(@RequestBody @Valid PropostaRequest request) {
        
        // Mapeamento manual simples (Poderia usar MapStruct em um projeto maior)
        Proposta proposta = Proposta.builder()
                .clienteNome(request.clienteNome())
                .clienteEmail(request.clienteEmail())
                .itens(request.itens().stream().map(itemDto -> ItemProposta.builder()
                        .nome(itemDto.nome())
                        .quantidade(itemDto.quantidade())
                        .precoUnitario(itemDto.precoUnitario())
                        .build()).collect(Collectors.toList()))
                .build();

        Proposta propostaCriada = propostaService.criarProposta(proposta);
        return ResponseEntity.status(HttpStatus.CREATED).body(propostaCriada);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Proposta> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(propostaService.buscarPorId(id));
    }

    @GetMapping
    public ResponseEntity<List<Proposta>> buscarPorCliente(@RequestParam String clienteNome) {
        return ResponseEntity.ok(propostaService.buscarPorCliente(clienteNome));
    }
}