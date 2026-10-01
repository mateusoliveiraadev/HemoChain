#include <stdio.h>
#include <stdlib.h>

#include "../include/estoque.h"

void estoque_inicializar(Estoque *estoque) {
    estoque->inicio = NULL;
    estoque->totalBolsas = 0;
}

int estoque_inserirBolsa(Estoque *estoque, Bolsa bolsa) {
    NoEstoque *novo = (NoEstoque *) malloc(sizeof(NoEstoque));
    if (novo == NULL) {
        return 0; /* falha de alocacao: sem memoria disponivel */
    }

    novo->dados = bolsa;
    novo->proximo = estoque->inicio; /* insercao no inicio: O(1), sem percorrer a lista */
    estoque->inicio = novo;
    estoque->totalBolsas++;

    return 1;
}

int estoque_removerBolsa(Estoque *estoque, int idBolsa) {
    NoEstoque *atual = estoque->inicio;
    NoEstoque *anterior = NULL;

    while (atual != NULL) {
        if (atual->dados.id == idBolsa) {
            if (anterior == NULL) {
                estoque->inicio = atual->proximo; /* removendo o primeiro no */
            } else {
                anterior->proximo = atual->proximo; /* "pula" o no removido */
            }
            free(atual);
            estoque->totalBolsas--;
            return 1;
        }
        anterior = atual;
        atual = atual->proximo;
    }

    return 0; /* id nao encontrado */
}

Bolsa *estoque_buscarBolsa(Estoque *estoque, int idBolsa) {
    NoEstoque *atual = estoque->inicio;

    while (atual != NULL) {
        if (atual->dados.id == idBolsa) {
            return &(atual->dados);
        }
        atual = atual->proximo;
    }

    return NULL;
}

void estoque_listar(const Estoque *estoque) {
    NoEstoque *atual = estoque->inicio;

    printf("Estoque (%d bolsa(s)):\n", estoque->totalBolsas);
    while (atual != NULL) {
        printf("  bolsa #%d | tipo=%d | componente=%d | %dml | coleta=%s\n",
               atual->dados.id,
               atual->dados.tipoSanguineo,
               atual->dados.tipoComponente,
               atual->dados.quantidadeMl,
               atual->dados.dataColeta);
        atual = atual->proximo;
    }
}

void estoque_liberar(Estoque *estoque) {
    NoEstoque *atual = estoque->inicio;
    NoEstoque *proximo;

    while (atual != NULL) {
        proximo = atual->proximo;
        free(atual);
        atual = proximo;
    }

    estoque->inicio = NULL;
    estoque->totalBolsas = 0;
}
