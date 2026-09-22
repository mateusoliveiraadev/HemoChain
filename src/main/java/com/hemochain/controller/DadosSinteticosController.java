package com.hemochain.controller;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.hemochain.dto.ResultadoGeracaoDados;
import com.hemochain.service.DadosSinteticosService;

/**
 * Endpoint auxiliar (fora do escopo funcional do sistema) para gerar a
 * massa de dados sintetica usada nas medicoes do trabalho - ex.:
 * POST /admin/dados-sinteticos?quantidade=1000000&hospitais=300
 */
@RestController
@RequestMapping("/admin/dados-sinteticos")
public class DadosSinteticosController {

    private static final long LIMITE_POR_CHAMADA = 2_000_000;

    private final DadosSinteticosService service;

    public DadosSinteticosController(DadosSinteticosService service) {
        this.service = service;
    }

    @PostMapping
    public ResultadoGeracaoDados gerar(
            @RequestParam long quantidade,
            @RequestParam(defaultValue = "200") int hospitais,
            @RequestHeader(value = "X-Perfil", required = false) String perfil) {
        verificarAcesso(perfil);
        if (quantidade <= 0 || quantidade > LIMITE_POR_CHAMADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "quantidade deve estar entre 1 e " + LIMITE_POR_CHAMADA + ".");
        }
        return service.gerar(quantidade, hospitais);
    }

    private void verificarAcesso(String perfil) {
        if (perfil == null || !"GESTOR_HEMOCENTRO".equals(perfil)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil sem acesso a geracao de dados.");
        }
    }
}
