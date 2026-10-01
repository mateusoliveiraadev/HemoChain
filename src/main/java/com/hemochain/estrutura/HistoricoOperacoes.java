package com.hemochain.estrutura;

import java.util.ArrayList;
import java.util.List;

/**
 * Historico de operacoes do sistema - equivalente Java de
 * pilha.c/pilha.h: uma PILHA (LIFO) em lista encadeada, acesso
 * sempre pelo topo.
 *
 * O historico so precisa responder "qual foi a ultima operacao" para
 * auditoria - por isso pilha, e nao lista ou fila.
 */
public class HistoricoOperacoes {

    private static final class NoPilha {
        private final OperacaoHistorico dados;
        private final NoPilha anterior;

        private NoPilha(OperacaoHistorico dados, NoPilha anterior) {
            this.dados = dados;
            this.anterior = anterior;
        }
    }

    private NoPilha topo;
    private int totalOperacoes;

    /** Empilha uma nova operacao no topo. */
    public void empilhar(OperacaoHistorico operacao) {
        topo = new NoPilha(operacao, topo); // o topo atual passa a ficar "abaixo" do novo
        totalOperacoes++;
    }

    /** Remove e retorna a operacao do topo. Retorna null se a pilha estiver vazia. */
    public OperacaoHistorico desempilhar() {
        if (topo == null) {
            return null;
        }

        OperacaoHistorico removida = topo.dados;
        topo = topo.anterior;
        totalOperacoes--;

        return removida;
    }

    /** Retorna a operacao do topo, sem remove-la. Null se vazia. */
    public OperacaoHistorico consultarTopo() {
        return topo == null ? null : topo.dados;
    }

    /** Lista o historico do topo (mais recente) para a base (mais antiga). */
    public List<OperacaoHistorico> listar() {
        List<OperacaoHistorico> resultado = new ArrayList<>();
        NoPilha atual = topo;

        while (atual != null) {
            resultado.add(atual.dados);
            atual = atual.anterior;
        }

        return resultado;
    }

    public int getTotalOperacoes() {
        return totalOperacoes;
    }
}
