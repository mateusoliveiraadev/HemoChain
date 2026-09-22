package com.hemochain.dto;

import com.hemochain.entity.TipoSanguineo;

/**
 * Projecao enxuta de uma requisicao, usada apenas para o calculo dos
 * indicadores de demanda (US06). Traz somente os campos que o algoritmo
 * de agregacao realmente le - tipo sanguineo e quantidade - para que a
 * consulta ao banco nao hidrate a entidade completa (hospital associado,
 * datas, urgencia etc.), mantendo o custo de carregamento o menor
 * possivel e isolando o que de fato queremos medir: o processamento.
 */
public record RequisicaoProjecao(TipoSanguineo tipoSanguineo, int quantidade) {
}
