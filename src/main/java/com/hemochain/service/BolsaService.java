package com.hemochain.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.hemochain.entity.*;
import com.hemochain.repository.*;

@Service
public class BolsaService {
    private final BolsaRepository bolsas;
    private final AlertaValidadeRepository alertas;
    private final ValidadeService validade;

    public BolsaService(BolsaRepository bolsas, AlertaValidadeRepository alertas, ValidadeService validade) {
        this.bolsas = bolsas;
        this.alertas = alertas;
        this.validade = validade;
    }

    @Transactional
    public BolsaHemocomponente cadastrar(BolsaHemocomponente bolsa) {
        bolsa.setCodigo(bolsa.getCodigo().trim().toUpperCase(java.util.Locale.ROOT));
        if (bolsas.existsByCodigo(bolsa.getCodigo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Codigo de bolsa ja cadastrado.");
        }
        bolsa.setValidade(validade.calcular(bolsa.getTipoComponente(), bolsa.getDataColeta()));
        bolsa.setStatus(StatusBolsa.DISPONIVEL);
        bolsas.saveAndFlush(bolsa);
        validade.verificarBolsa(bolsa, LocalDate.now());
        return bolsa;
    }

    public List<BolsaHemocomponente> listar(StatusBolsa status) {
        validade.verificar(LocalDate.now());
        return status == null ? bolsas.findAllByOrderByValidadeAscIdAsc()
                : bolsas.findByStatusOrderByValidadeAscIdAsc(status);
    }

    public BolsaHemocomponente buscar(Long id) {
        validade.verificar(LocalDate.now());
        return bolsas.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Bolsa nao encontrada."));
    }

    @Transactional
    public BolsaHemocomponente atualizar(Long id, BolsaHemocomponente dados) {
        BolsaHemocomponente bolsa = bolsas.buscarParaAtualizacao(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Bolsa nao encontrada."));
        if (bolsa.getStatus() == StatusBolsa.DESCARTADO || !bolsa.getValidade().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bolsa vencida ou descartada nao pode ser alterada.");
        }
        String codigo = dados.getCodigo().trim().toUpperCase(java.util.Locale.ROOT);
        if (bolsas.existsByCodigoAndIdNot(codigo, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Codigo de bolsa ja cadastrado.");
        }
        bolsa.setCodigo(codigo);
        bolsa.setTipoComponente(dados.getTipoComponente());
        bolsa.setTipoSanguineo(dados.getTipoSanguineo());
        bolsa.setVolumeMl(dados.getVolumeMl());
        bolsa.setDataColeta(dados.getDataColeta());
        bolsa.setValidade(validade.calcular(bolsa.getTipoComponente(), bolsa.getDataColeta()));
        alertas.deleteByBolsaId(id);
        alertas.flush();
        validade.verificarBolsa(bolsa, LocalDate.now());
        return bolsa;
    }
}
