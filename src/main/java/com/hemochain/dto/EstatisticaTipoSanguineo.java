package com.hemochain.dto;

import com.hemochain.entity.TipoSanguineo;

/**
 * Estatistica descritiva de demanda de um tipo sanguineo especifico,
 * calculada sobre o historico de requisicoes (US06 - Painel de
 * indicadores de estoque e demanda).
 */
public record EstatisticaTipoSanguineo(
        TipoSanguineo tipoSanguineo,
        long totalRequisicoes,
        long somaQuantidade,
        double media,
        double variancia,
        double desvioPadrao) {
}
