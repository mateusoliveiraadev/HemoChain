package com.hemochain.service;

import java.util.ArrayList;
import java.util.List;

import com.hemochain.dto.EstatisticaTipoSanguineo;
import com.hemochain.entity.TipoSanguineo;

/**
 * Acumulador dos indicadores de demanda por tipo sanguineo.
 *
 * Cada instancia mantem, por tipo sanguineo (indexado por ordinal, sem
 * HashMap), a contagem de requisicoes, a soma das quantidades e a soma
 * dos quadrados das quantidades - o suficiente para derivar media e
 * variancia ao final (formula de soma de quadrados: var = E[X^2] - E[X]^2).
 *
 * Na versao sequencial existe um unico acumulador para todo o historico.
 * Na versao paralela, cada thread processa sua fatia em um acumulador
 * proprio (sem escrita compartilhada, logo sem necessidade de lock) e os
 * acumuladores parciais sao combinados com {@link #mesclar} ao final -
 * uma operacao O(K), com K = numero de tipos sanguineos (constante = 8).
 * Soma e contagem sao associativas e comutativas, entao a ordem de
 * combinacao nao afeta o resultado.
 */
public final class DemandaAccumulator {

    private static final int TIPOS = TipoSanguineo.values().length;

    private final long[] contagem = new long[TIPOS];
    private final long[] soma = new long[TIPOS];
    private final double[] somaQuadrados = new double[TIPOS];

    public void acumular(TipoSanguineo tipo, int quantidade) {
        int i = tipo.ordinal();
        contagem[i]++;
        soma[i] += quantidade;
        somaQuadrados[i] += (double) quantidade * (double) quantidade;
    }

    /** Combina os totais de outro acumulador (etapa de reducao/merge). */
    public void mesclar(DemandaAccumulator outro) {
        for (int i = 0; i < TIPOS; i++) {
            this.contagem[i] += outro.contagem[i];
            this.soma[i] += outro.soma[i];
            this.somaQuadrados[i] += outro.somaQuadrados[i];
        }
    }

    /** Converte os totais acumulados nas estatisticas finais por tipo sanguineo. */
    public List<EstatisticaTipoSanguineo> finalizar() {
        List<EstatisticaTipoSanguineo> resultado = new ArrayList<>(TIPOS);
        for (TipoSanguineo tipo : TipoSanguineo.values()) {
            int i = tipo.ordinal();
            long n = contagem[i];
            double media = n == 0 ? 0.0 : (double) soma[i] / n;
            double variancia = n == 0 ? 0.0 : (somaQuadrados[i] / n) - (media * media);
            variancia = Math.max(variancia, 0.0); // corrige ruido de ponto flutuante quando variancia ~ 0
            double desvioPadrao = Math.sqrt(variancia);
            resultado.add(new EstatisticaTipoSanguineo(tipo, n, soma[i], media, variancia, desvioPadrao));
        }
        return resultado;
    }
}
