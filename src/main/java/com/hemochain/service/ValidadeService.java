package com.hemochain.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.hemochain.entity.*;
import com.hemochain.repository.*;

@Service
public class ValidadeService {
    private final BolsaRepository bolsas;
    private final AlertaValidadeRepository alertas;

    @Value("${estoque.validade.hemacias:35}")
    private int diasHemacias;
    @Value("${estoque.validade.plasma:365}")
    private int diasPlasma;
    @Value("${estoque.validade.plaquetas:5}")
    private int diasPlaquetas;
    @Value("${estoque.validade.crioprecipitado:365}")
    private int diasCrioprecipitado;
    @Value("${estoque.alertas.dias:5,2}")
    private int[] limiares;

    public ValidadeService(BolsaRepository bolsas, AlertaValidadeRepository alertas) {
        this.bolsas = bolsas;
        this.alertas = alertas;
    }

    @PostConstruct
    public void validarConfiguracao() {
        if (diasHemacias < 1 || diasPlasma < 1 || diasPlaquetas < 1 || diasCrioprecipitado < 1) {
            throw new IllegalArgumentException("Prazos didaticos devem ser positivos.");
        }
        if (limiares.length == 0) {
            throw new IllegalArgumentException("Informe pelo menos um limiar de alerta.");
        }
        for (int dias : limiares) {
            if (dias < 1) {
                throw new IllegalArgumentException("Limiares de alerta devem ser positivos.");
            }
        }
    }

    // Tabela exclusivamente didatica. Nao representa protocolo clinico.
    public LocalDate calcular(TipoComponente componente, LocalDate coleta) {
        int dias;
        switch (componente) {
            case CONCENTRADO_HEMACIAS: dias = diasHemacias; break;
            case PLASMA: dias = diasPlasma; break;
            case PLAQUETAS: dias = diasPlaquetas; break;
            case CRIOPRECIPITADO: dias = diasCrioprecipitado; break;
            default: throw new IllegalArgumentException("Componente desconhecido.");
        }
        return coleta.plusDays(dias);
    }

    @Transactional
    public void verificar(LocalDate hoje) {
        List<BolsaHemocomponente> disponiveis = bolsas.buscarParaVerificacao(StatusBolsa.DISPONIVEL);
        for (BolsaHemocomponente bolsa : disponiveis) {
            verificarBolsa(bolsa, hoje);
        }
    }

    // Chamado dentro da transacao de cadastro/edicao ou da verificacao.
    public void verificarBolsa(BolsaHemocomponente bolsa, LocalDate hoje) {
        if (bolsa.getStatus() != StatusBolsa.DISPONIVEL) {
            return;
        }
        if (!bolsa.getValidade().isAfter(hoje)) {
            bolsa.descartar(LocalDateTime.now());
            return;
        }
        long diasRestantes = ChronoUnit.DAYS.between(hoje, bolsa.getValidade());
        for (int limiar : limiares) {
            if (diasRestantes <= limiar && !alertas.existsByBolsaIdAndLimiarDias(bolsa.getId(), limiar)) {
                alertas.save(new AlertaValidade(bolsa, limiar, LocalDateTime.now()));
            }
        }
    }

    @Transactional
    public List<AlertaValidade> listarAlertas() {
        verificar(LocalDate.now());
        return alertas.findByBolsaStatusOrderByDataCriacaoDesc(StatusBolsa.DISPONIVEL);
    }
}
