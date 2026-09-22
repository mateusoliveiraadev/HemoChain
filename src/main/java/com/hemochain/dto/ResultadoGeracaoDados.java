package com.hemochain.dto;

/** Resultado da geracao de massa sintetica de requisicoes, usada para os testes de carga (100 mil / 1 milhao). */
public record ResultadoGeracaoDados(long requisicoesGeradas, int hospitaisUtilizados, long tempoGeracaoMs) {
}
