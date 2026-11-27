package com.hemochain.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** SDD-01: invariantes locais, sem depender da validacao do service ou de persistencia. */
class EntregaHospitalarTest {
    private static final Instant ENTREGA = Instant.parse("2026-11-24T12:00:00Z");
    private static final Instant RECEBIMENTO = ENTREGA.plusSeconds(60);
    private static final Instant UTILIZACAO = RECEBIMENTO.plusSeconds(60);
    private static final String ORIGEM = "PERFIL:HOSPITAL_SOLICITANTE;HOSPITAL:1";

    private EntregaHospitalar entrega() {
        var requisicao = new RequisicaoHospitalar(1L, TipoComponente.CONCENTRADO_HEMACIAS,
                TipoSanguineo.O_NEGATIVO, 1, UrgenciaRequisicao.ROTINA, LocalDate.of(2026, 11, 25));
        return new EntregaHospitalar(requisicao, new BolsaHemocomponente(),
                ENTREGA, ENTREGA.plusSeconds(86400));
    }

    @Test
    void registraRecebimentoIntegroComOsDadosInformados() {
        var entrega = entrega();

        entrega.receber(CondicaoRecebimento.INTEGRA, "Embalagem integra", RECEBIMENTO, ORIGEM);

        assertAll(
                () -> assertEquals(CondicaoRecebimento.INTEGRA, entrega.getCondicao()),
                () -> assertEquals("Embalagem integra", entrega.getObservacao()),
                () -> assertEquals(RECEBIMENTO, entrega.getRecebidaEm()),
                () -> assertEquals(ORIGEM, entrega.getOrigemRecebimento()));
    }

    @Test
    void recebimentoIntegroPermiteObservacaoAusente() {
        var entrega = entrega();

        entrega.receber(CondicaoRecebimento.INTEGRA, null, RECEBIMENTO, ORIGEM);

        assertEquals(RECEBIMENTO, entrega.getRecebidaEm());
        assertNull(entrega.getObservacao());
    }

    @Test
    void segundaConfirmacaoNaoSobrescreveOPrimeiroRecebimento() {
        var entrega = entrega();
        entrega.receber(CondicaoRecebimento.INTEGRA, "Original", RECEBIMENTO, ORIGEM);

        assertThrows(IllegalStateException.class, () -> entrega.receber(
                CondicaoRecebimento.NAO_CONFORME, "Outra confirmacao", UTILIZACAO, "OUTRA_ORIGEM"));

        assertAll(
                () -> assertEquals(CondicaoRecebimento.INTEGRA, entrega.getCondicao()),
                () -> assertEquals("Original", entrega.getObservacao()),
                () -> assertEquals(RECEBIMENTO, entrega.getRecebidaEm()),
                () -> assertEquals(ORIGEM, entrega.getOrigemRecebimento()));
    }

    @Test
    void condicaoAusenteNaoMarcaRecebimento() {
        var entrega = entrega();

        assertThrows(IllegalArgumentException.class,
                () -> entrega.receber(null, "Observacao", RECEBIMENTO, ORIGEM));

        assertSemRecebimento(entrega);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void naoConformidadeSemObservacaoNaoMarcaRecebimento(String observacao) {
        var entrega = entrega();

        assertThrows(IllegalArgumentException.class, () -> entrega.receber(
                CondicaoRecebimento.NAO_CONFORME, observacao, RECEBIMENTO, ORIGEM));

        assertSemRecebimento(entrega);
    }

    @Test
    void usoAntesDoRecebimentoNaoAlteraAEntrega() {
        var entrega = entrega();

        assertThrows(IllegalStateException.class, () -> entrega.utilizar(UTILIZACAO, ORIGEM));

        assertNull(entrega.getUtilizadaEm());
        assertNull(entrega.getOrigemUtilizacao());
    }

    @Test
    void recebimentoNaoConformeNaoPermiteUso() {
        var entrega = entrega();
        entrega.receber(CondicaoRecebimento.NAO_CONFORME, "Embalagem violada", RECEBIMENTO, ORIGEM);

        assertThrows(IllegalStateException.class, () -> entrega.utilizar(UTILIZACAO, ORIGEM));

        assertAll(
                () -> assertNull(entrega.getUtilizadaEm()),
                () -> assertNull(entrega.getOrigemUtilizacao()),
                () -> assertEquals(CondicaoRecebimento.NAO_CONFORME, entrega.getCondicao()),
                () -> assertEquals("Embalagem violada", entrega.getObservacao()),
                () -> assertEquals(RECEBIMENTO, entrega.getRecebidaEm()),
                () -> assertEquals(ORIGEM, entrega.getOrigemRecebimento()));
    }

    @Test
    void recebimentoIntegroHabilitaUtilizacao() {
        var entrega = entrega();
        entrega.receber(CondicaoRecebimento.INTEGRA, null, RECEBIMENTO, ORIGEM);

        entrega.utilizar(UTILIZACAO, ORIGEM);

        assertEquals(UTILIZACAO, entrega.getUtilizadaEm());
        assertEquals(ORIGEM, entrega.getOrigemUtilizacao());
    }

    @Test
    void segundaUtilizacaoPreservaOPrimeiroRegistro() {
        var entrega = entrega();
        entrega.receber(CondicaoRecebimento.INTEGRA, null, RECEBIMENTO, ORIGEM);
        entrega.utilizar(UTILIZACAO, ORIGEM);

        assertThrows(IllegalStateException.class,
                () -> entrega.utilizar(UTILIZACAO.plusSeconds(60), "OUTRA_ORIGEM"));

        assertEquals(UTILIZACAO, entrega.getUtilizadaEm());
        assertEquals(ORIGEM, entrega.getOrigemUtilizacao());
    }

    private void assertSemRecebimento(EntregaHospitalar entrega) {
        assertAll(
                () -> assertNull(entrega.getCondicao()),
                () -> assertNull(entrega.getObservacao()),
                () -> assertNull(entrega.getRecebidaEm()),
                () -> assertNull(entrega.getOrigemRecebimento()));
    }

    @Test
    void instanteAusenteNaoPermiteRecebimentoSemMarcaDeOcorrencia() {
        var entrega = entrega();

        assertThrows(IllegalArgumentException.class,
                () -> entrega.receber(CondicaoRecebimento.INTEGRA, "Original", null, ORIGEM));

        assertSemRecebimento(entrega);
    }

    @Test
    void instanteAusenteNaoPermiteUsoSemMarcaDeOcorrencia() {
        var entrega = entrega();
        entrega.receber(CondicaoRecebimento.INTEGRA, null, RECEBIMENTO, ORIGEM);

        assertThrows(IllegalArgumentException.class, () -> entrega.utilizar(null, ORIGEM));

        assertNull(entrega.getUtilizadaEm());
        assertNull(entrega.getOrigemUtilizacao());
        assertEquals(RECEBIMENTO, entrega.getRecebidaEm());
    }
}
