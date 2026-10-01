package com.hemochain.estrutura;

import java.util.ArrayList;
import java.util.List;

/**
 * Estoque de bolsas de sangue, implementado como uma LISTA ENCADEADA
 * manual - equivalente Java de estoque.c/estoque.h.
 *
 * Deliberadamente NAO usa java.util.LinkedList: o ponto da unidade e
 * implementar a propria estrutura, nao delegar para a biblioteca
 * padrao. O No interno (classe privada NoEstoque) e o analogo direto
 * do "struct NoEstoque { Bolsa dados; struct NoEstoque *proximo; }"
 * em C - so que em vez de um ponteiro manipulado com malloc/free, o
 * campo "proximo" e uma referencia de objeto, e quem libera a
 * memoria de um no removido/descartado e o garbage collector do
 * Java (ver documento de equivalencia).
 *
 * Sem dependencias de persistencia (JPA) ou do Spring: a classe pode
 * ser usada por qualquer camada da aplicacao (service, controller,
 * teste) sem travar a forma como o estoque sera, no futuro,
 * persistido.
 */
public class EstoqueBolsas {

    private static final class NoEstoque {
        private final Bolsa dados;
        private NoEstoque proximo;

        private NoEstoque(Bolsa dados, NoEstoque proximo) {
            this.dados = dados;
            this.proximo = proximo;
        }
    }

    private NoEstoque inicio;
    private int totalBolsas;

    /** Insere uma nova bolsa no inicio da lista. */
    public void inserirBolsa(Bolsa bolsa) {
        inicio = new NoEstoque(bolsa, inicio); // insercao no inicio: O(1), sem percorrer a lista
        totalBolsas++;
    }

    /** Remove a bolsa com o id informado. Retorna true se encontrou e removeu, false caso contrario. */
    public boolean removerBolsa(int idBolsa) {
        NoEstoque atual = inicio;
        NoEstoque anterior = null;

        while (atual != null) {
            if (atual.dados.getId() == idBolsa) {
                if (anterior == null) {
                    inicio = atual.proximo; // removendo o primeiro no
                } else {
                    anterior.proximo = atual.proximo; // "pula" o no removido
                }
                totalBolsas--;
                return true;
            }
            anterior = atual;
            atual = atual.proximo;
        }

        return false; // id nao encontrado
    }

    /** Retorna a bolsa com o id informado, ou null se nao existir. */
    public Bolsa buscarBolsa(int idBolsa) {
        NoEstoque atual = inicio;

        while (atual != null) {
            if (atual.dados.getId() == idBolsa) {
                return atual.dados;
            }
            atual = atual.proximo;
        }

        return null;
    }

    /** Lista todas as bolsas do estoque, na ordem da lista encadeada. */
    public List<Bolsa> listar() {
        List<Bolsa> resultado = new ArrayList<>();
        NoEstoque atual = inicio;

        while (atual != null) {
            resultado.add(atual.dados);
            atual = atual.proximo;
        }

        return resultado;
    }

    public int getTotalBolsas() {
        return totalBolsas;
    }
}
