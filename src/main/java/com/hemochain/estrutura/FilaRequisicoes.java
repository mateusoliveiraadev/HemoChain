package com.hemochain.estrutura;

import java.util.ArrayList;
import java.util.List;

/**
 * Fila (FIFO) de requisicoes hospitalares - equivalente Java de
 * fila.c/fila.h: lista encadeada com ponteiros de inicio e fim,
 * para enfileirar e desenfileirar em O(1).
 *
 * As requisicoes sao atendidas estritamente na ordem de chegada.
 * Nenhuma reordenacao por urgencia, prazo ou rota acontece aqui -
 * isso e roteirizacao e fica para a Unidade 2.
 */
public class FilaRequisicoes {

    private static final class NoFila {
        private final Requisicao dados;
        private NoFila proximo;

        private NoFila(Requisicao dados) {
            this.dados = dados;
            this.proximo = null;
        }
    }

    private NoFila inicio;
    private NoFila fim;
    private int totalRequisicoes;

    /** Insere a requisicao no fim da fila. */
    public void enfileirar(Requisicao requisicao) {
        NoFila novo = new NoFila(requisicao);

        if (fim == null) {
            // fila estava vazia: o novo no e tanto o inicio quanto o fim
            inicio = novo;
            fim = novo;
        } else {
            fim.proximo = novo; // encadeia no fim atual
            fim = novo;         // e passa a ser o novo fim
        }

        totalRequisicoes++;
    }

    /** Remove e retorna a requisicao do inicio da fila. Retorna null se a fila estiver vazia. */
    public Requisicao desenfileirar() {
        if (inicio == null) {
            return null; // fila vazia, nada para atender
        }

        Requisicao removida = inicio.dados;
        inicio = inicio.proximo;
        if (inicio == null) {
            fim = null; // a fila ficou vazia
        }

        totalRequisicoes--;
        return removida;
    }

    /** Retorna a proxima requisicao a ser atendida, sem remove-la (peek). Null se vazia. */
    public Requisicao consultarProxima() {
        return inicio == null ? null : inicio.dados;
    }

    /** Lista todas as requisicoes, na ordem em que serao atendidas. */
    public List<Requisicao> listar() {
        List<Requisicao> resultado = new ArrayList<>();
        NoFila atual = inicio;

        while (atual != null) {
            resultado.add(atual.dados);
            atual = atual.proximo;
        }

        return resultado;
    }

    public int getTotalRequisicoes() {
        return totalRequisicoes;
    }
}
