package com.hemochain.service;

import java.util.List;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.hemochain.entity.*;
import com.hemochain.repository.*;

@Service
public class RastreabilidadeService {
    private final EntityManager em;
    private final EventoBolsaRepository eventos;
    private final BolsaRepository bolsas;

    public RastreabilidadeService(EntityManager em, EventoBolsaRepository eventos, BolsaRepository bolsas) {
        this.em = em;
        this.eventos = eventos;
        this.bolsas = bolsas;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(BolsaHemocomponente bolsa, TipoEventoBolsa tipo, String origem, String detalhes) {
        em.persist(new EventoBolsa(bolsa, tipo, origem, detalhes));
    }

    @Transactional(readOnly = true)
    public List<EventoBolsa> historico(Long bolsaId) {
        if (!bolsas.existsById(bolsaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bolsa nao encontrada.");
        }
        return eventos.findByBolsa_IdOrderByMomentoAscIdAsc(bolsaId);
    }

    public static String retrato(BolsaHemocomponente b) {
        return "codigo=" + b.getCodigo() + "; componente=" + b.getTipoComponente()
                + "; ABO/Rh=" + b.getTipoSanguineo() + "; volumeMl=" + b.getVolumeMl()
                + "; coleta=" + b.getDataColeta() + "; validade=" + b.getValidade() + "; status=" + b.getStatus();
    }
}
