package com.hemochain.controller;

import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.hemochain.entity.RequisicaoHospitalar;
import com.hemochain.service.RequisicaoService;

@RestController
@RequestMapping("/requisicoes")
public class RequisicaoController {

    // Quem pode ver a lista de requisicoes (hospital ve as suas, gestor acompanha todas)
    private static final Set<String> PERFIS_LEITURA = Set.of("HOSPITAL_SOLICITANTE", "GESTOR_HEMOCENTRO");
    // Quem pode registrar uma nova requisicao (US02: e o hospital solicitante quem pede)
    private static final Set<String> PERFIS_ESCRITA = Set.of("HOSPITAL_SOLICITANTE");

    private final RequisicaoService service;

    public RequisicaoController(RequisicaoService service) {
        this.service = service;
    }

    @GetMapping
    public List<RequisicaoHospitalar> listar(@RequestHeader(value = "X-Perfil", required = false) String perfil) {
        verificarAcesso(perfil, PERFIS_LEITURA);
        return service.listarTodas();
    }

    @PostMapping
    public RequisicaoHospitalar cadastrar(@Valid @RequestBody RequisicaoHospitalar requisicao,
            @RequestHeader(value = "X-Perfil", required = false) String perfil) {
        verificarAcesso(perfil, PERFIS_ESCRITA);
        return service.cadastrar(requisicao);
    }

    private void verificarAcesso(String perfil, Set<String> perfisPermitidos) {
        if (perfil == null || !perfisPermitidos.contains(perfil)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil sem acesso a este recurso.");
        }
    }
}
