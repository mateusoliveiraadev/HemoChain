#include <stdio.h>
#include <stdlib.h>

#include "../include/fila.h"

void fila_inicializar(FilaRequisicoes *fila) {
    fila->inicio = NULL;
    fila->fim = NULL;
    fila->totalRequisicoes = 0;
}

int fila_enfileirar(FilaRequisicoes *fila, Requisicao requisicao) {
    NoFila *novo = (NoFila *) malloc(sizeof(NoFila));
    if (novo == NULL) {
        return 0;
    }

    novo->dados = requisicao;
    novo->proximo = NULL;

    if (fila->fim == NULL) {
        /* fila estava vazia: o novo no e tanto o inicio quanto o fim */
        fila->inicio = novo;
        fila->fim = novo;
    } else {
        fila->fim->proximo = novo; /* encadeia no fim atual */
        fila->fim = novo;          /* e passa a ser o novo fim */
    }

    fila->totalRequisicoes++;
    return 1;
}

int fila_desenfileirar(FilaRequisicoes *fila, Requisicao *saida) {
    if (fila->inicio == NULL) {
        return 0; /* fila vazia, nada para atender */
    }

    NoFila *removido = fila->inicio;
    *saida = removido->dados;

    fila->inicio = removido->proximo;
    if (fila->inicio == NULL) {
        fila->fim = NULL; /* a fila ficou vazia */
    }

    free(removido);
    fila->totalRequisicoes--;
    return 1;
}

Requisicao *fila_consultarProxima(FilaRequisicoes *fila) {
    if (fila->inicio == NULL) {
        return NULL;
    }
    return &(fila->inicio->dados);
}

void fila_listar(const FilaRequisicoes *fila) {
    NoFila *atual = fila->inicio;
    int posicao = 1;

    printf("Fila de requisicoes (%d na espera):\n", fila->totalRequisicoes);
    while (atual != NULL) {
        printf("  %do da fila -> requisicao #%d | hospital=%d | tipo=%d | componente=%d | qtd=%d | urgencia=%d\n",
               posicao,
               atual->dados.id,
               atual->dados.hospitalId,
               atual->dados.tipoSanguineo,
               atual->dados.tipoComponente,
               atual->dados.quantidade,
               atual->dados.urgencia);
        atual = atual->proximo;
        posicao++;
    }
}

void fila_liberar(FilaRequisicoes *fila) {
    NoFila *atual = fila->inicio;
    NoFila *proximo;

    while (atual != NULL) {
        proximo = atual->proximo;
        free(atual);
        atual = proximo;
    }

    fila->inicio = NULL;
    fila->fim = NULL;
    fila->totalRequisicoes = 0;
}
