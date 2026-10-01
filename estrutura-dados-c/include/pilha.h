#ifndef PILHA_H
#define PILHA_H

/*
 * Historico de operacoes do sistema, implementado como uma PILHA
 * (LIFO) em lista encadeada, alocacao manual.
 *
 * Escolha de estrutura: o historico so precisa responder "qual foi
 * a ultima operacao" (para auditoria/undo) - acesso sempre pelo
 * topo, nunca pelo meio ou pelo fim. Isso e exatamente uma pilha:
 * empilhar ao registrar, desempilhar/consultar ao auditar.
 */

typedef enum {
    BOLSA_INSERIDA,
    BOLSA_REMOVIDA,
    REQUISICAO_ENFILEIRADA,
    REQUISICAO_ATENDIDA
} TipoOperacao;

typedef struct {
    TipoOperacao tipo;
    int idReferencia; /* id da bolsa ou da requisicao envolvida na operacao */
    char descricao[80];
} OperacaoHistorico;

typedef struct NoPilha {
    OperacaoHistorico dados;
    struct NoPilha *anterior;
} NoPilha;

typedef struct {
    NoPilha *topo;
    int totalOperacoes;
} HistoricoOperacoes;

void historico_inicializar(HistoricoOperacoes *historico);

/* Empilha uma nova operacao no topo. Retorna 1 em sucesso, 0 se malloc falhar. */
int historico_empilhar(HistoricoOperacoes *historico, OperacaoHistorico operacao);

/* Remove a operacao do topo e copia para *saida. Retorna 1 se havia algo, 0 se a pilha estava vazia. */
int historico_desempilhar(HistoricoOperacoes *historico, OperacaoHistorico *saida);

/* Retorna ponteiro para a operacao do topo, sem remove-la. NULL se vazia. */
OperacaoHistorico *historico_consultarTopo(HistoricoOperacoes *historico);

/* Imprime o historico do topo (mais recente) para a base (mais antiga). */
void historico_listar(const HistoricoOperacoes *historico);

/* Libera todos os nos alocados dinamicamente. */
void historico_liberar(HistoricoOperacoes *historico);

#endif
