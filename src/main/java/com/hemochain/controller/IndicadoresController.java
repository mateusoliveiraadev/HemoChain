package com.hemochain.controller;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.hemochain.dto.IndicadoresDemandaResponse;
import com.hemochain.service.RequisicaoIndicadoresService;

/**
 * Endpoint real do painel de indicadores de demanda (US06). A requisicao
 * HTTP chega, o servidor carrega o historico e calcula as estatisticas
 * (sequencial ou com threads, conforme o parametro {@code threads}), e a
 * resposta volta com o resultado e os metadados de tempo usados nas
 * medicoes do trabalho.
 */
@RestController
@RequestMapping("/requisicoes/indicadores")
public class IndicadoresController {

    // Painel de indicadores e visao gerencial: so o gestor do hemocentro acessa (US06 / US08).
    private static final Set<String> PERFIS_COM_ACESSO = Set.of("GESTOR_HEMOCENTRO");

    private final RequisicaoIndicadoresService service;

    public IndicadoresController(RequisicaoIndicadoresService service) {
        this.service = service;
    }

    /**
     * GET /requisicoes/indicadores?threads=4&desde=2026-01-01T00:00:00
     *
     * threads=1 (padrao) executa a versao sequencial; threads>1 executa a
     * versao com ExecutorService, particionando os dados em N fatias.
     */
    @GetMapping
    public IndicadoresDemandaResponse indicadores(
            @RequestParam(defaultValue = "1") int threads,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestHeader(value = "X-Perfil", required = false) String perfil) {
        verificarAcesso(perfil);
        return service.calcular(threads, desde);
    }

    private void verificarAcesso(String perfil) {
        if (perfil == null || !PERFIS_COM_ACESSO.contains(perfil)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil sem acesso ao painel de indicadores.");
        }
    }
}
