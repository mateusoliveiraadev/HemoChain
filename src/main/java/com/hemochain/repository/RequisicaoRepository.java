package com.hemochain.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hemochain.dto.RequisicaoProjecao;
import com.hemochain.entity.RequisicaoHospitalar;

public interface RequisicaoRepository extends JpaRepository<RequisicaoHospitalar, Long> {
    List<RequisicaoHospitalar> findAllByOrderByDataCriacaoDesc();

    /**
     * Projecao enxuta (so tipoSanguineo e quantidade) usada pelo calculo dos
     * indicadores de demanda (US06). Evita hidratar a entidade completa
     * (associacao com Hospital, datas, urgencia) para nao inflar o custo de
     * carregamento de uma operacao cujo gargalo estudado e o processamento,
     * nao a consulta - ver justificativa, secao 1.3.
     */
    @Query("select new com.hemochain.dto.RequisicaoProjecao(r.tipoSanguineo, r.quantidade) "
            + "from RequisicaoHospitalar r "
            + "where (:desde is null or r.dataCriacao >= :desde)")
    List<RequisicaoProjecao> buscarProjecaoParaIndicadores(@Param("desde") LocalDateTime desde);
}
