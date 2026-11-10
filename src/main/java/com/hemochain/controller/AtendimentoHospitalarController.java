package com.hemochain.controller;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.hemochain.entity.*;
import com.hemochain.service.AtendimentoHospitalarService;

@RestController
@RequestMapping("/requisicoes")
public class AtendimentoHospitalarController {
    private final AtendimentoHospitalarService atendimento;
    public AtendimentoHospitalarController(AtendimentoHospitalarService atendimento) { this.atendimento = atendimento; }
    public record DadosEntrega(@NotNull @Positive Long bolsaId, @AssertTrue boolean entregaRealizada) { }
    public record DadosRecebimento(@NotNull CondicaoRecebimento condicao, @Size(max = 1000) String observacao) { }

    @PostMapping("/{id}/entregas") @ResponseStatus(HttpStatus.CREATED)
    public EntregaHospitalar entregar(@PathVariable Long id, @Valid @RequestBody DadosEntrega dados,
            @RequestHeader(value = "X-Perfil", required = false) String perfil) {
        exigirPerfil(perfil, "GESTOR_HEMOCENTRO");
        return atendimento.registrarEntrega(id, dados.bolsaId());
    }

    @GetMapping("/{id}/entregas")
    public List<EntregaHospitalar> listar(@PathVariable Long id,
            @RequestHeader(value = "X-Perfil", required = false) String perfil,
            @RequestHeader(value = "X-Hospital-Id", required = false) Long hospitalId) {
        exigirPerfil(perfil, "GESTOR_HEMOCENTRO", "HOSPITAL_SOLICITANTE");
        return atendimento.listar(id, "GESTOR_HEMOCENTRO".equals(perfil), hospitalId);
    }

    @PostMapping("/{id}/entregas/{entregaId}/recebimento")
    public EntregaHospitalar receber(@PathVariable Long id, @PathVariable Long entregaId,
            @Valid @RequestBody DadosRecebimento dados,
            @RequestHeader(value = "X-Perfil", required = false) String perfil,
            @RequestHeader(value = "X-Hospital-Id", required = false) Long hospitalId) {
        exigirPerfil(perfil, "HOSPITAL_SOLICITANTE");
        return atendimento.receber(id, entregaId, hospitalId, dados.condicao(), dados.observacao());
    }

    @PostMapping("/{id}/entregas/{entregaId}/utilizacao")
    public EntregaHospitalar utilizar(@PathVariable Long id, @PathVariable Long entregaId,
            @RequestHeader(value = "X-Perfil", required = false) String perfil,
            @RequestHeader(value = "X-Hospital-Id", required = false) Long hospitalId) {
        exigirPerfil(perfil, "HOSPITAL_SOLICITANTE");
        return atendimento.utilizar(id, entregaId, hospitalId);
    }

    @GetMapping("/pendencias")
    public List<EntregaHospitalar> pendencias(@RequestHeader(value = "X-Perfil", required = false) String perfil) {
        exigirPerfil(perfil, "GESTOR_HEMOCENTRO", "OPERADOR_LOGISTICA");
        return atendimento.verificarPendencias();
    }

    private static void exigirPerfil(String perfil, String... permitidos) {
        if (perfil == null || !List.of(permitidos).contains(perfil)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil sem acesso a esta operacao.");
        }
    }
}
