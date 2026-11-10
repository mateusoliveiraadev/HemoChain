package com.hemochain.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.hemochain.entity.EntregaHospitalar;

public interface EntregaHospitalarRepository extends JpaRepository<EntregaHospitalar, Long> {
    List<EntregaHospitalar> findByRequisicaoIdOrderByEntregueEmAscIdAsc(Long requisicaoId);
    @Query("select distinct e.requisicao.id from EntregaHospitalar e where e.recebidaEm is null "
            + "and e.prazoConfirmacao < :agora order by e.requisicao.id")
    List<Long> requisicoesAtrasadas(Instant agora);
    List<EntregaHospitalar> findByRecebidaEmIsNullAndAlertaEmIsNotNullOrderByPrazoConfirmacaoAsc();
}
