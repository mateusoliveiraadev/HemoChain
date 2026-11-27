package com.hemochain.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Gera um erro 5xx de proposito para demonstrar painel de erros, log de nivel ERROR e trace
 * correlacionado. Existe apenas com o perfil "observabilidade-demo"; a API do Compose de demo usa loopback.
 */
@Profile("observabilidade-demo")
@RestController
@RequestMapping("/observabilidade")
public class ErroSimuladoController {
    private static final Logger log = LoggerFactory.getLogger(ErroSimuladoController.class);

    @GetMapping("/erro-simulado")
    public void erroSimulado() {
        log.error("Falha simulada para demonstracao de observabilidade");
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "erro simulado");
    }
}
