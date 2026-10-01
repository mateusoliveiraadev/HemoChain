package com.hemochain.estrutura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.hemochain.entity.TipoComponente;
import com.hemochain.entity.TipoSanguineo;
import com.hemochain.entity.UrgenciaRequisicao;

class FilaRequisicoesTest {

    @Test
    void comecaVazia() {
        FilaRequisicoes fila = new FilaRequisicoes();

        assertEquals(0, fila.getTotalRequisicoes());
        assertNull(fila.consultarProxima());
        assertNull(fila.desenfileirar());
    }

    @Test
    void atendeNaOrdemDeChegadaMesmoComUrgenciasDiferentes() {
        FilaRequisicoes fila = new FilaRequisicoes();
        Requisicao r1 = new Requisicao(10, 1, TipoComponente.CONCENTRADO_HEMACIAS, TipoSanguineo.O_NEGATIVO, 2, UrgenciaRequisicao.EMERGENCIAL);
        Requisicao r2 = new Requisicao(11, 2, TipoComponente.PLASMA, TipoSanguineo.A_POSITIVO, 1, UrgenciaRequisicao.ROTINA);
        Requisicao r3 = new Requisicao(12, 1, TipoComponente.PLAQUETAS, TipoSanguineo.B_NEGATIVO, 3, UrgenciaRequisicao.URGENTE);

        fila.enfileirar(r1);
        fila.enfileirar(r2);
        fila.enfileirar(r3);
        assertEquals(3, fila.getTotalRequisicoes());

        // FIFO puro: a proxima e sempre a que chegou primeiro, nunca a de maior urgencia (proibido na Unidade 1)
        assertEquals(10, fila.consultarProxima().getId());

        assertEquals(10, fila.desenfileirar().getId());
        assertEquals(2, fila.getTotalRequisicoes());

        assertEquals(11, fila.desenfileirar().getId());
        assertEquals(12, fila.desenfileirar().getId());
        assertEquals(0, fila.getTotalRequisicoes());
    }

    @Test
    void listarRespeitaOrdemDaFila() {
        FilaRequisicoes fila = new FilaRequisicoes();
        fila.enfileirar(new Requisicao(1, 1, TipoComponente.PLASMA, TipoSanguineo.AB_POSITIVO, 1, UrgenciaRequisicao.ROTINA));
        fila.enfileirar(new Requisicao(2, 1, TipoComponente.PLASMA, TipoSanguineo.AB_POSITIVO, 1, UrgenciaRequisicao.ROTINA));

        assertEquals(2, fila.listar().size());
        assertEquals(1, fila.listar().get(0).getId());
        assertEquals(2, fila.listar().get(1).getId());
    }
}
