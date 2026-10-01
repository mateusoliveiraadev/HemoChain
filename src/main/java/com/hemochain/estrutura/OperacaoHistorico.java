package com.hemochain.estrutura;

/**
 * Um registro do historico de operacoes - equivalente Java do struct
 * OperacaoHistorico em C (pilha.h). Usa um record por ser um dado
 * imutavel e sem comportamento proprio, exatamente como o struct.
 */
public record OperacaoHistorico(TipoOperacao tipo, int idReferencia, String descricao) {
}
