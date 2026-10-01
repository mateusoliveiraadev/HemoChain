#ifndef FILA_H
#define FILA_H

#include "bolsa.h"

/*
 * Requisicoes hospitalares, implementadas como uma FILA (FIFO) em
 * lista encadeada com ponteiros de inicio e fim, alocacao manual.
 *
 * Escolha de estrutura: requisicoes devem ser atendidas na ordem de
 * chegada (primeira a chegar, primeira a ser retirada da fila) - e
 * exatamente a semantica de uma fila. Nao ha, nesta unidade,
 * nenhuma reordenacao por urgencia, prazo ou rota: isso e
 * roteirizacao e fica para a Unidade 2. Aqui a fila e pura.
 */

typedef struct {
    int id;
    int hospitalId;
    TipoComponente tipoComponente;
    TipoSanguineo tipoSanguineo;
    int quantidade;
    UrgenciaRequisicao urgencia;
} Requisicao;

typedef struct NoFila {
    Requisicao dados;
    struct NoFila *proximo;
} NoFila;

typedef struct {
    NoFila *inicio;
    NoFila *fim;
    int totalRequisicoes;
} FilaRequisicoes;

void fila_inicializar(FilaRequisicoes *fila);

/* Insere a requisicao no fim da fila. Retorna 1 em sucesso, 0 se malloc falhar. */
int fila_enfileirar(FilaRequisicoes *fila, Requisicao requisicao);

/* Remove a requisicao do inicio da fila e copia para *saida. Retorna 1 se havia algo, 0 se a fila estava vazia. */
int fila_desenfileirar(FilaRequisicoes *fila, Requisicao *saida);

/* Retorna ponteiro para a proxima requisicao a ser atendida, sem remove-la (peek). NULL se vazia. */
Requisicao *fila_consultarProxima(FilaRequisicoes *fila);

/* Imprime todas as requisicoes, na ordem em que serao atendidas. */
void fila_listar(const FilaRequisicoes *fila);

/* Libera todos os nos alocados dinamicamente. */
void fila_liberar(FilaRequisicoes *fila);

#endif
