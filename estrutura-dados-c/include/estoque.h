#ifndef ESTOQUE_H
#define ESTOQUE_H

#include "bolsa.h"

/*
 * Estoque de bolsas de sangue, implementado como uma LISTA ENCADEADA
 * simples, com alocacao manual (malloc) e liberacao manual (free).
 *
 * Escolha de estrutura: o estoque cresce e encolhe o tempo todo
 * (bolsas chegam, bolsas saem) e nao precisamos de acesso aleatorio
 * por posicao - so inserir, remover por id e percorrer. Uma lista
 * encadeada cobre isso sem desperdicar memoria com um vetor de
 * tamanho fixo nem exigir realocacao.
 */

typedef struct NoEstoque {
    Bolsa dados;
    struct NoEstoque *proximo;
} NoEstoque;

typedef struct {
    NoEstoque *inicio;
    int totalBolsas;
} Estoque;

void estoque_inicializar(Estoque *estoque);

/* Insere uma nova bolsa no inicio da lista. Retorna 1 em sucesso, 0 se malloc falhar. */
int estoque_inserirBolsa(Estoque *estoque, Bolsa bolsa);

/* Remove a bolsa com o id informado. Retorna 1 se encontrou e removeu, 0 caso contrario. */
int estoque_removerBolsa(Estoque *estoque, int idBolsa);

/* Retorna ponteiro para a bolsa com o id informado, ou NULL se nao existir. */
Bolsa *estoque_buscarBolsa(Estoque *estoque, int idBolsa);

/* Imprime todas as bolsas do estoque, na ordem da lista. */
void estoque_listar(const Estoque *estoque);

/* Libera todos os nos alocados dinamicamente (percorre e da free em cada um). */
void estoque_liberar(Estoque *estoque);

#endif
