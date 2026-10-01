#include <stdio.h>
#include <stdlib.h>

#include "../include/pilha.h"

void historico_inicializar(HistoricoOperacoes *historico) {
    historico->topo = NULL;
    historico->totalOperacoes = 0;
}

int historico_empilhar(HistoricoOperacoes *historico, OperacaoHistorico operacao) {
    NoPilha *novo = (NoPilha *) malloc(sizeof(NoPilha));
    if (novo == NULL) {
        return 0;
    }

    novo->dados = operacao;
    novo->anterior = historico->topo; /* o topo atual passa a ficar "abaixo" do novo */
    historico->topo = novo;
    historico->totalOperacoes++;

    return 1;
}

int historico_desempilhar(HistoricoOperacoes *historico, OperacaoHistorico *saida) {
    if (historico->topo == NULL) {
        return 0; /* pilha vazia */
    }

    NoPilha *removido = historico->topo;
    *saida = removido->dados;

    historico->topo = removido->anterior;
    free(removido);
    historico->totalOperacoes--;

    return 1;
}

OperacaoHistorico *historico_consultarTopo(HistoricoOperacoes *historico) {
    if (historico->topo == NULL) {
        return NULL;
    }
    return &(historico->topo->dados);
}

void historico_listar(const HistoricoOperacoes *historico) {
    NoPilha *atual = historico->topo;

    printf("Historico de operacoes (%d registro(s), do mais recente ao mais antigo):\n",
           historico->totalOperacoes);
    while (atual != NULL) {
        printf("  [tipo=%d] ref=#%d - %s\n",
               atual->dados.tipo,
               atual->dados.idReferencia,
               atual->dados.descricao);
        atual = atual->anterior;
    }
}

void historico_liberar(HistoricoOperacoes *historico) {
    NoPilha *atual = historico->topo;
    NoPilha *anterior;

    while (atual != NULL) {
        anterior = atual->anterior;
        free(atual);
        atual = anterior;
    }

    historico->topo = NULL;
    historico->totalOperacoes = 0;
}
