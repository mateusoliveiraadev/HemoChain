package com.hemochain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.util.UUID;
import com.hemochain.entity.*;
import com.hemochain.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

@SpringBootTest(properties = "spring.jpa.show-sql=false")
class RastreabilidadeTransacaoTest {
    @Autowired BolsaService bolsas;
    @Autowired BolsaRepository repository;
    @Autowired AtendimentoHospitalarService atendimento;
    @Autowired RequisicaoService requisicoes;
    @Autowired HospitalService hospitais;
    @Autowired EntregaHospitalarRepository entregas;
    @SpyBean RastreabilidadeService rastreabilidade;

    private BolsaHemocomponente dados() {
        BolsaHemocomponente b = new BolsaHemocomponente();
        b.setCodigo("TX-" + UUID.randomUUID()); b.setTipoComponente(TipoComponente.CONCENTRADO_HEMACIAS);
        b.setTipoSanguineo(TipoSanguineo.O_NEGATIVO); b.setVolumeMl(450); b.setDataColeta(LocalDate.now());
        return b;
    }

    @Test void falhaNoHistoricoReverteCadastro() {
        var b = dados();
        RastreabilidadeService alvo = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(rastreabilidade);
        doThrow(new IllegalStateException("Falha de persistencia induzida pelo teste"))
                .when(alvo).registrar(any(), any(), anyString(), anyString());
        try {
            assertThrows(IllegalStateException.class, () -> bolsas.cadastrar(b));
            assertFalse(repository.existsByCodigo(b.getCodigo()));
        } finally { reset(alvo); }
    }

    @Test void falhaNoHistoricoReverteEntregaEStatus() {
        var b = bolsas.cadastrar(dados());
        var h = hospitais.cadastrar(new Hospital("Hospital TX sintetico", "SINTETICO", "0000"));
        var r = requisicoes.cadastrar(new RequisicaoHospitalar(h.getId(), TipoComponente.CONCENTRADO_HEMACIAS,
                TipoSanguineo.O_NEGATIVO, 1, UrgenciaRequisicao.ROTINA, LocalDate.now().plusDays(1)));
        RastreabilidadeService alvo = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(rastreabilidade);
        doThrow(new IllegalStateException("Falha de persistencia induzida pelo teste"))
                .when(alvo).registrar(any(), any(), anyString(), anyString());
        try {
            assertThrows(IllegalStateException.class, () -> atendimento.registrarEntrega(r.getId(), b.getId()));
            assertEquals(StatusBolsa.DISPONIVEL, repository.findById(b.getId()).orElseThrow().getStatus());
            assertTrue(entregas.findByRequisicaoIdOrderByEntregueEmAscIdAsc(r.getId()).isEmpty());
            assertEquals(1, rastreabilidade.historico(b.getId()).size());
        } finally { reset(alvo); }
    }

    @Test void falhaNoHistoricoReverteRecebimentoEUtilizacao() {
        var b = bolsas.cadastrar(dados());
        var h = hospitais.cadastrar(new Hospital("Hospital TX recebimento", "SINTETICO", "0000"));
        var r = requisicoes.cadastrar(new RequisicaoHospitalar(h.getId(), TipoComponente.CONCENTRADO_HEMACIAS,
                TipoSanguineo.O_NEGATIVO, 1, UrgenciaRequisicao.ROTINA, LocalDate.now().plusDays(1)));
        var e = atendimento.registrarEntrega(r.getId(),b.getId());
        RastreabilidadeService alvo = org.springframework.test.util.AopTestUtils.getUltimateTargetObject(rastreabilidade);
        doThrow(new IllegalStateException("Falha induzida"))
                .when(alvo).registrar(any(),any(),anyString(),anyString());
        try {
            assertThrows(IllegalStateException.class, () -> atendimento.receber(r.getId(),e.getId(),h.getId(),CondicaoRecebimento.INTEGRA,""));
            assertNull(entregas.findById(e.getId()).orElseThrow().getRecebidaEm());
            assertEquals(StatusBolsa.ENTREGUE,repository.findById(b.getId()).orElseThrow().getStatus());
        } finally {reset(alvo);}
        atendimento.receber(r.getId(),e.getId(),h.getId(),CondicaoRecebimento.INTEGRA,"");
        doThrow(new IllegalStateException("Falha induzida"))
                .when(alvo).registrar(any(),any(),anyString(),anyString());
        try {
            assertThrows(IllegalStateException.class, () -> atendimento.utilizar(r.getId(),e.getId(),h.getId()));
            assertNull(entregas.findById(e.getId()).orElseThrow().getUtilizadaEm());
            assertEquals(StatusBolsa.RECEBIDA,repository.findById(b.getId()).orElseThrow().getStatus());
            assertEquals(3,rastreabilidade.historico(b.getId()).size());
        } finally {reset(alvo);}
    }
}
