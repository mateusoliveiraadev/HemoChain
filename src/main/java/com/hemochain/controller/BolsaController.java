package com.hemochain.controller;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.hemochain.entity.*;
import com.hemochain.service.*;

@RestController
@RequestMapping("/bolsas")
public class BolsaController {
    private final BolsaService bolsas;
    private final ValidadeService validade;

    public BolsaController(BolsaService bolsas, ValidadeService validade) {
        this.bolsas = bolsas;
        this.validade = validade;
    }

    @GetMapping
    public List<BolsaHemocomponente> listar(@RequestHeader(value = "X-Perfil", required = false) String perfil,
            @RequestParam(required = false) StatusBolsa status) {
        verificarPerfil(perfil);
        return bolsas.listar(status);
    }

    @GetMapping("/{id}")
    public BolsaHemocomponente buscar(@PathVariable Long id,
            @RequestHeader(value = "X-Perfil", required = false) String perfil) {
        verificarPerfil(perfil);
        return bolsas.buscar(id);
    }

    @GetMapping("/alertas")
    public List<AlertaValidade> alertas(@RequestHeader(value = "X-Perfil", required = false) String perfil) {
        verificarPerfil(perfil);
        return validade.listarAlertas();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BolsaHemocomponente cadastrar(@Valid @RequestBody BolsaHemocomponente bolsa,
            @RequestHeader(value = "X-Perfil", required = false) String perfil) {
        verificarPerfil(perfil);
        return bolsas.cadastrar(bolsa);
    }

    @PutMapping("/{id}")
    public BolsaHemocomponente atualizar(@PathVariable Long id, @Valid @RequestBody BolsaHemocomponente bolsa,
            @RequestHeader(value = "X-Perfil", required = false) String perfil) {
        verificarPerfil(perfil);
        return bolsas.atualizar(id, bolsa);
    }

    private void verificarPerfil(String perfil) {
        if (!"GESTOR_HEMOCENTRO".equals(perfil)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente o gestor pode acessar o estoque.");
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public java.util.Map<String, String> dadosInvalidos(MethodArgumentNotValidException erro) {
        String campos = erro.getBindingResult().getFieldErrors().stream()
                .map(campo -> campo.getField()).distinct().sorted().collect(Collectors.joining(", "));
        return java.util.Map.of("message", "Confira os campos obrigatorios ou invalidos: " + campos);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public java.util.Map<String, String> conflito() {
        return java.util.Map.of("message", "Conflito nos dados. Confira se o codigo da bolsa ja existe.");
    }
}
