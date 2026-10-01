#include <assert.h>
#include <stdio.h>
#include <string.h>

#include "../include/bolsa.h"
#include "../include/estoque.h"
#include "../include/fila.h"
#include "../include/pilha.h"

/*
 * Programa de demonstracao e teste das tres estruturas do dominio
 * "Rota Vital" (Unidade 1): estoque (lista), requisicoes (fila) e
 * historico de operacoes (pilha).
 *
 * Cada operacao de negocio (inserir bolsa, remover bolsa, enfileirar
 * requisicao, atender requisicao) tambem empilha um registro no
 * historico - e assim que as tres estruturas trabalham juntas.
 *
 * Os asserts abaixo validam que cada estrutura inicializa, insere,
 * remove e consulta corretamente, servindo como suite de testes do
 * lado C (equivalente aos testes JUnit do lado Java).
 */

static void registrarOperacao(HistoricoOperacoes *historico, TipoOperacao tipo, int idReferencia, const char *descricao) {
    OperacaoHistorico operacao;
    operacao.tipo = tipo;
    operacao.idReferencia = idReferencia;
    strncpy(operacao.descricao, descricao, sizeof(operacao.descricao) - 1);
    operacao.descricao[sizeof(operacao.descricao) - 1] = '\0';

    int sucesso = historico_empilhar(historico, operacao);
    assert(sucesso == 1);
}

static void testarEstoque(HistoricoOperacoes *historico) {
    Estoque estoque;
    estoque_inicializar(&estoque);
    assert(estoque.totalBolsas == 0);
    assert(estoque_buscarBolsa(&estoque, 1) == NULL);

    Bolsa b1 = {1, O_NEGATIVO, CONCENTRADO_HEMACIAS, 450, "2026-09-01"};
    Bolsa b2 = {2, A_POSITIVO, PLASMA, 300, "2026-09-05"};

    assert(estoque_inserirBolsa(&estoque, b1) == 1);
    registrarOperacao(historico, BOLSA_INSERIDA, b1.id, "bolsa #1 (O-) inserida no estoque");
    assert(estoque_inserirBolsa(&estoque, b2) == 1);
    registrarOperacao(historico, BOLSA_INSERIDA, b2.id, "bolsa #2 (A+) inserida no estoque");
    assert(estoque.totalBolsas == 2);

    Bolsa *encontrada = estoque_buscarBolsa(&estoque, 2);
    assert(encontrada != NULL);
    assert(encontrada->tipoSanguineo == A_POSITIVO);
    assert(encontrada->quantidadeMl == 300);

    assert(estoque_removerBolsa(&estoque, 1) == 1);
    registrarOperacao(historico, BOLSA_REMOVIDA, b1.id, "bolsa #1 (O-) removida do estoque");
    assert(estoque.totalBolsas == 1);
    assert(estoque_buscarBolsa(&estoque, 1) == NULL);     /* nao existe mais */
    assert(estoque_removerBolsa(&estoque, 999) == 0);     /* id inexistente */

    estoque_listar(&estoque);
    estoque_liberar(&estoque);
    assert(estoque.totalBolsas == 0);
    assert(estoque.inicio == NULL);

    printf("[OK] estoque (lista encadeada)\n");
}

static void testarFila(HistoricoOperacoes *historico) {
    FilaRequisicoes fila;
    fila_inicializar(&fila);
    assert(fila.totalRequisicoes == 0);

    Requisicao r1 = {10, 1, CONCENTRADO_HEMACIAS, O_NEGATIVO, 2, EMERGENCIAL};
    Requisicao r2 = {11, 2, PLASMA, A_POSITIVO, 1, ROTINA};
    Requisicao r3 = {12, 1, PLAQUETAS, B_NEGATIVO, 3, URGENTE};

    assert(fila_enfileirar(&fila, r1) == 1);
    registrarOperacao(historico, REQUISICAO_ENFILEIRADA, r1.id, "requisicao #10 entrou na fila");
    assert(fila_enfileirar(&fila, r2) == 1);
    registrarOperacao(historico, REQUISICAO_ENFILEIRADA, r2.id, "requisicao #11 entrou na fila");
    assert(fila_enfileirar(&fila, r3) == 1);
    registrarOperacao(historico, REQUISICAO_ENFILEIRADA, r3.id, "requisicao #12 entrou na fila");
    assert(fila.totalRequisicoes == 3);

    /* FIFO puro: a proxima a ser atendida e sempre a primeira que chegou (r1), nunca por urgencia */
    Requisicao *proxima = fila_consultarProxima(&fila);
    assert(proxima != NULL);
    assert(proxima->id == 10);

    Requisicao atendida;
    assert(fila_desenfileirar(&fila, &atendida) == 1);
    assert(atendida.id == 10);
    registrarOperacao(historico, REQUISICAO_ATENDIDA, atendida.id, "requisicao #10 atendida e saiu da fila");
    assert(fila.totalRequisicoes == 2);

    assert(fila_desenfileirar(&fila, &atendida) == 1);
    assert(atendida.id == 11); /* continua respeitando ordem de chegada, nao de urgencia */
    registrarOperacao(historico, REQUISICAO_ATENDIDA, atendida.id, "requisicao #11 atendida e saiu da fila");

    fila_listar(&fila);
    fila_liberar(&fila);
    assert(fila.totalRequisicoes == 0);
    assert(fila.inicio == NULL && fila.fim == NULL);

    Requisicao descartavel;
    assert(fila_desenfileirar(&fila, &descartavel) == 0); /* fila vazia */

    printf("[OK] fila de requisicoes (FIFO)\n");
}

static void testarHistorico(HistoricoOperacoes *historico) {
    /* neste ponto o historico ja recebeu 8 operacoes das funcoes anteriores:
       3 insercoes + 1 remocao no estoque, e 3 enfileiramentos + 2 atendimentos na fila */
    assert(historico->totalOperacoes == 8);

    OperacaoHistorico *topo = historico_consultarTopo(historico);
    assert(topo != NULL);
    assert(topo->tipo == REQUISICAO_ATENDIDA);
    assert(topo->idReferencia == 11); /* ultima operacao empilhada foi a requisicao #11 */

    historico_listar(historico);

    OperacaoHistorico removida;
    int totalAntes = historico->totalOperacoes;
    assert(historico_desempilhar(historico, &removida) == 1);
    assert(removida.idReferencia == 11);
    assert(historico->totalOperacoes == totalAntes - 1);

    historico_liberar(historico);
    assert(historico->totalOperacoes == 0);
    assert(historico->topo == NULL);

    OperacaoHistorico descartavel;
    assert(historico_desempilhar(historico, &descartavel) == 0); /* pilha vazia */

    printf("[OK] historico de operacoes (pilha)\n");
}

int main(void) {
    HistoricoOperacoes historico;
    historico_inicializar(&historico);

    testarEstoque(&historico);
    testarFila(&historico);
    testarHistorico(&historico); /* consome e libera o historico ao final */

    printf("\nTodos os testes passaram.\n");
    return 0;
}
