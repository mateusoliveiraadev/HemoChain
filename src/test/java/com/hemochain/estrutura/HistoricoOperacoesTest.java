package com.hemochain.estrutura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class HistoricoOperacoesTest {

    @Test
    void comecaVazio() {
        HistoricoOperacoes historico = new HistoricoOperacoes();

        assertEquals(0, historico.getTotalOperacoes());
        assertNull(historico.consultarTopo());
        assertNull(historico.desempilhar());
    }

    @Test
    void consultaOTopoERetiraNaOrdemLifo() {
        HistoricoOperacoes historico = new HistoricoOperacoes();
        historico.empilhar(new OperacaoHistorico(TipoOperacao.BOLSA_INSERIDA, 1, "bolsa #1 inserida"));
        historico.empilhar(new OperacaoHistorico(TipoOperacao.BOLSA_INSERIDA, 2, "bolsa #2 inserida"));
        historico.empilhar(new OperacaoHistorico(TipoOperacao.BOLSA_REMOVIDA, 1, "bolsa #1 removida"));

        assertEquals(3, historico.getTotalOperacoes());

        // a ultima operacao empilhada (bolsa #1 removida) e a primeira a aparecer - LIFO
        assertEquals(1, historico.consultarTopo().idReferencia());
        assertEquals(TipoOperacao.BOLSA_REMOVIDA, historico.consultarTopo().tipo());

        OperacaoHistorico topo = historico.desempilhar();
        assertEquals(TipoOperacao.BOLSA_REMOVIDA, topo.tipo());
        assertEquals(2, historico.getTotalOperacoes());

        OperacaoHistorico anterior = historico.desempilhar();
        assertEquals(2, anterior.idReferencia()); // "bolsa #2 inserida", empilhada antes da remocao
    }

    @Test
    void listarVaiDoTopoParaABase() {
        HistoricoOperacoes historico = new HistoricoOperacoes();
        historico.empilhar(new OperacaoHistorico(TipoOperacao.REQUISICAO_ENFILEIRADA, 10, "requisicao #10 entrou na fila"));
        historico.empilhar(new OperacaoHistorico(TipoOperacao.REQUISICAO_ATENDIDA, 10, "requisicao #10 atendida"));

        assertEquals(2, historico.listar().size());
        assertEquals(TipoOperacao.REQUISICAO_ATENDIDA, historico.listar().get(0).tipo()); // topo primeiro
        assertEquals(TipoOperacao.REQUISICAO_ENFILEIRADA, historico.listar().get(1).tipo());
    }
}
