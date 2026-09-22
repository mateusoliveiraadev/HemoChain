package com.hemochain.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resposta do endpoint de indicadores de demanda. Alem das estatisticas
 * por tipo sanguineo, devolve os metadados de execucao (threads usadas
 * e tempo de calculo) para que as medicoes do trabalho possam ser
 * coletadas diretamente da resposta HTTP, sem instrumentacao externa.
 */
public record IndicadoresDemandaResponse(
        List<EstatisticaTipoSanguineo> estatisticas,
        long totalRequisicoesProcessadas,
        int threadsUtilizadas,
        long tempoCalculoNanos,
        double tempoCalculoMs,
        LocalDateTime desdeFiltro) {
}
