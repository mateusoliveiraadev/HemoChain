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

import com.hemochain.entity.Hospital;
import com.hemochain.service.HospitalService;

@RestController
@RequestMapping("/hospitais")
public class HospitalController {

    // Controle de acesso minimo: o front envia o perfil do usuario logado no
    // cabecalho X-Perfil. Sem Spring Security, sem sessao - so uma checagem simples.
    private static final Set<String> PERFIS_COM_ACESSO = Set.of("HOSPITAL_SOLICITANTE", "GESTOR_HEMOCENTRO");

    private final HospitalService service;

    public HospitalController(HospitalService service) {
        this.service = service;
    }

    @GetMapping
    public List<Hospital> listar(@RequestHeader(value = "X-Perfil", required = false) String perfil) {
        verificarAcesso(perfil);
        return service.listarTodos();
    }

    @PostMapping
    public Hospital cadastrar(@Valid @RequestBody Hospital hospital,
            @RequestHeader(value = "X-Perfil", required = false) String perfil) {
        verificarAcesso(perfil);
        return service.cadastrar(hospital);
    }

    private void verificarAcesso(String perfil) {
        if (perfil == null || !PERFIS_COM_ACESSO.contains(perfil)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Perfil sem acesso as funcionalidades hospitalares.");
        }
    }
}
